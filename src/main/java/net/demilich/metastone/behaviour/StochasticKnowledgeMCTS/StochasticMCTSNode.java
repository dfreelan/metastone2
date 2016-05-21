package net.demilich.metastone.behaviour.StochasticKnowledgeMCTS;

import net.demilich.metastone.bahaviour.ModifiedMCTS.*;
import net.demilich.metastone.game.behaviour.experimentalMCTS.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Hashtable;
import net.demilich.metastone.game.Environment;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.actions.PlayCardAction;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.PlayAllRandomBiasedBehavior;
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.FeatureCollector;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;
import net.demilich.metastone.game.targeting.CardReference;
import org.apache.commons.math3.distribution.NormalDistribution;

/*
 *
 *  largely inspired from (as copy-pasted then heavily modified)
 *  http://mcts.ai/code/java.html
 *
 */
public class StochasticMCTSNode {

    static Random r = new Random();

    CardCollection preShuffledDeck;
    static double epsilon = 1e-6;

    List<StochasticMCTSNode> children = null;
    double nVisits;
    double totValue[] = new double[2];
    GameContext context;
    List<GameAction> actions = null;
    //IGameStateHeuristic h = new ThreatBasedHeuristic(FeatureVector.getDefault());
    double hValue = 0.0;
    int activePlayer = -1;
    int winningPlayer = -1;
    double exploreFactor = 1.0;
    GameAction action = null;
    private boolean firstEndTurn = false;
    boolean isABattleCryNode = false;
    GameCritique critique;


    NormalDistribution stateEvaluation = null;
    double virtualVisits = 1.0;
    FeatureCollector f;
    //CardReference prevCardReference;
    public StochasticMCTSNode(GameContext simulation, double exploreFactor, boolean firstEndTurn, FeatureCollector f, GameCritique critique) {
        this.context = simulation;
        this.exploreFactor = exploreFactor;
        this.f = f;
        this.critique = critique;
    }

    boolean amRoot = false;
    boolean beingCalledFromResolve = false;
    boolean actionApplied = false;
    public void selectAction() {
        //context.play();
        List<StochasticMCTSNode> visited = new LinkedList<StochasticMCTSNode>();
        StochasticMCTSNode cur = this;
        amRoot = true;
        visited.add(this);
        while (!cur.isLeaf()) {
            cur = cur.select();
            visited.add(cur);
        }

        double value;
        //cur.preservedContext = cur.context.clone();
        if (!cur.context.gameDecided()) {
            cur.applyAction();
        }
        if (!cur.context.gameDecided()) {
            cur.expand(cur.context.getActivePlayerId());
            StochasticMCTSNode newNode = cur.select();
            visited.add(newNode);

            if (!(newNode.action.getActionType()==ActionType.BATTLECRY)) {
                value = rollOut(newNode, newNode.action);
            } else {
                //System.err.println("i selection node with action " + newNode.action.getTargetKey());
                //System.err.println("i got to this state via action: " + action);
                value = this.rollOutBattleCry(newNode, newNode.action);
            }
        } else if (cur.context.getWinningPlayerId() == 0 || cur.context.getWinningPlayerId() == 1) {
            if (cur.context.getWinningPlayerId() == 0) {
                totValue[0] = Double.POSITIVE_INFINITY;
                totValue[1] = Double.NEGATIVE_INFINITY;
            } else {
                totValue[1] = Double.POSITIVE_INFINITY;
                totValue[0] = Double.NEGATIVE_INFINITY;
            }
            value = cur.context.getWinningPlayerId();
        } else {
            totValue[0] = Double.NEGATIVE_INFINITY;
            totValue[1] = Double.NEGATIVE_INFINITY;
            value = -1;
        }

        for (StochasticMCTSNode node : visited) {
            node.updateStats(value);
        }
    }

    public double getCost(GameAction action) {
        //if (action != null && isCard(action)) {
        //    return 0.027;
        //}

        return 0.0;
    }

    public boolean isCard(GameAction randomAction) {
        return (randomAction.getActionType() == ActionType.SUMMON || randomAction.getActionType() == ActionType.SPELL || randomAction.getActionType() == ActionType.EQUIP_WEAPON);

    }
    Hashtable<Integer, StochasticMCTSNode> childHash = null;

    public int hash(StochasticMCTSNode state) {
        return state.context.toString().hashCode();
    }
    private void initEvaluation(GameContext resultingContext, int pid){
        double netEval = critique.getCritique(resultingContext, context.getPlayer(pid));
        this.stateEvaluation = new NormalDistribution(netEval,.16);
    }
    public double sampleEval(){

        applyAction();
        double value = -100;
        int count = 0;
        while(value<0 || value > 1){
            value = stateEvaluation.sample();
            if(count>10000){
                System.err.println("took too long");
                System.exit(0);
            }
            count++;
        }
        if(this.activePlayer == 0){
            value = 1-value;
        }
        return value;
    }

    public void applyAction() {

        if (action == null || actionApplied) {//null should only really happen for the root node
            return;
        }
        actionApplied = true;
        int origionalPID = context.getActivePlayerId();
        this.activePlayer = origionalPID;
        if(origionalPID == -1)
        System.err.println("orig pid" + origionalPID);
        //this.context = this.context.clone();
        if(action.getActionType() == ActionType.BATTLECRY){
            context.getLogic().battlecries = null;
            performBattlecryAction(context,action);
        }else{
            context.getLogic().simulationActive = true;
            context.getLogic().battlecries = null;
            context.getLogic().performGameAction(context.getActivePlayerId(), action);
            context.getLogic().simulationActive = false;
        }
        if (action.getActionType() == ActionType.END_TURN) {
            context.startTurn(context.getActivePlayerId());
        }
        if (context.getLogic().battlecries != null) {
            //System.err.println("battle cry action happened during simulation!!!!! after applying action " + action);
            actions = context.getLogic().battlecries;
            context.getLogic().battlecries = null;
        } else {
            actions = context.getValidActions();
        }
        this.initEvaluation(context,origionalPID);
    }

    public void expand(int playerID) {
        assert (actions != null);
        assert (actions.size() > 0);
        if (actions.size() == 0) {
            throw new RuntimeException("There were 0 actions supplied after applying action " + action + "in context " + this.context);
        }
        ArrayList<StochasticMCTSNode> newNodes = new ArrayList<StochasticMCTSNode>(actions.size());

        for (GameAction action : actions) {
            StochasticMCTSNode newNode = new StochasticMCTSNode(context.clone(), exploreFactor, firstEndTurn,f,critique);

            newNode.action = action;

            newNodes.add(newNode);
            if(this.beingCalledFromResolve){
                newNode.isABattleCryNode = true;
            }
        }
        children = newNodes;
        this.activePlayer = context.getActivePlayerId();




    }
    private double getMCTSValue(double expValue,double nVisits, double virtualValue){
        double exploitParam = (this.virtualVisits*virtualValue + nVisits*expValue);
        exploitParam/= (nVisits+this.virtualVisits);

        double exploreParam = + exploreFactor * (Math.sqrt(Math.log(nVisits + this.virtualVisits + 1) / (nVisits + this.virtualVisits)));
        ///if(exploitParam + exploreParam)
        return exploitParam + exploreParam;


    }
    private StochasticMCTSNode select() {
        StochasticMCTSNode selected = children.get(0);

        double bestValue = getMCTSValue(selected.totValue[activePlayer],selected.nVisits,selected.sampleEval());

        for (int i = 1; i < children.size(); i++) {
            StochasticMCTSNode c = children.get(i);
            double uctValue = getMCTSValue(c.totValue[activePlayer],c.nVisits,c.sampleEval());
            // small random number to break ties randomly in unexpanded nodes
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        //System.err.println("i selected action " + actions.get(bestIndex));

        return selected;
    }

    public boolean isLeaf() {
        return children == null;
    }

    public double rollOut(StochasticMCTSNode tn, GameAction battlecry) {
        //play a random game of hearthstone...

        GameContext simulation = tn.context.clone();

        int pid = activePlayer;


       // simulation.getLogic().performGameAction(simulation.getActivePlayerId(), battlecry);

        double neuralValue = critique.getCritique(simulation, simulation.getPlayer(pid));
         if(Double.isNaN(neuralValue)){

             System.err.println("neural network value was null");
             System.exit(0);
         }
        double oldValue = neuralValue;
        int count = 0;
        do{
            neuralValue =  new NormalDistribution(oldValue,.2).sample();
            count++;
            if(count>10000){
                System.err.println("count take too long 2 "  + oldValue);
                System.exit(0);
            }        
        }while(neuralValue <0 || neuralValue > 1);

         if(Double.isNaN(neuralValue)){

             System.err.println("nnormalDist returned null becuse apache sucks balls");
             System.exit(0);
         }


        if(pid == 0){
            neuralValue = 1-neuralValue;
        }
        //simulation.getLogic().rolloutActive = true;
        //simulation.getLogic().checkForDeadEntities();
        //simulation.playFromMiddle();

        //double playthroughValue = simulation.getWinningPlayerId();

        return neuralValue;

    }
    Random generator = new Random();
    public double rollOutBattleCry(StochasticMCTSNode tn, GameAction battlecry) {
        //play a random game of hearthstone... but battlecry first
        //System.err.println("if you get in here it's invalid");
        GameContext simulation = tn.context.clone();

        int pid = activePlayer;
        //simulation.getLogic().rolloutActive = true;
//        performBattlecryAction(simulation,battlecry);

        double neuralValue = critique.getCritique(simulation,simulation.getPlayer(pid));
        double oldValue = neuralValue;
        int count = 0;
          do{
            neuralValue =  new NormalDistribution(oldValue,.2).sample();
            count++;
            if(count>10000){
                System.err.println("count take too long 2 "  + oldValue);
                System.exit(0);
            }        
        }while(neuralValue <0 || neuralValue > 1);

        if(pid == 0){
            neuralValue = 1-neuralValue;
        }

        //simulation.playFromMiddle();

       // double playthroughValue = simulation.getWinningPlayerId();

        return neuralValue;
    }
    public void performBattlecryAction(GameContext simulation, GameAction battlecry){
         boolean resolvedLate = simulation.getLogic().minion.getBattlecry().isResolvedLate();

        simulation.getLogic().performGameAction(simulation.getActivePlayerId(), battlecry);
        simulation.getLogic().checkForDeadEntities();

        if(resolvedLate){
            simulation.getLogic().afterBattlecryLate();
        }else{
            simulation.getLogic().afterBattlecry();
        }

        simulation.getLogic().afterCardPlayed(context.getActivePlayerId(), simulation.getLogic().source.getCardReference());
        simulation.getEnvironment().remove(Environment.PENDING_CARD);

        simulation.getEnvironment().remove(Environment.TARGET);


        simulation.getLogic().minion = null;
        simulation.getLogic().resolveBattlecry = false;

    }
    public void updateStats(double value) {
        nVisits++;
        if (value != -1.0) {
            totValue[0] += 1-value;
            totValue[1] += value;
        }
    }

    public int arity() {
        return children == null ? 0 : children.size();
    }

    public GameAction getBestAction() {
        double best = Double.NEGATIVE_INFINITY;
        GameAction bestAction = null;
        if (children == null) {
            return null;
        }

        for (int i = 0; i < children.size(); i++) {
            StochasticMCTSNode child = children.get(i);

//System.err.println("CHILD TOT VALLUE " + child.totValue[child.context.getActivePlayerId()] + " " + "best " + best);
            double fitness = child.totValue[activePlayer] / (1 + child.nVisits);

            //System.err.println("action is " + actions.get(i) + ": " + fitness + " " + child.nVisits);
            if (fitness > best) {
                bestAction = actions.get(i);
                best = fitness;
            }

        }
 //       System.err.println("best action: " + bestAction + " " + best);
        return bestAction;

    }

    public StochasticMCTSNode getBestChild() {
        double best = Double.NEGATIVE_INFINITY;
        StochasticMCTSNode bestChild = null;
        //if (children.size() == 0) {
        //   throw new RuntimeException("NO CHILDREN");
        //   }
        if (children == null) {
            return null;
        }
        for (int i = 0; i < children.size(); i++) {
            StochasticMCTSNode child = children.get(i);
            //System.err.println("CHILD TOT VALLUE " + child.totValue[child.context.getActivePlayerId()] + " " + "best " + best);
            double fitness = child.totValue[activePlayer] / (1 + child.nVisits);
            if (fitness > best && child.nVisits > 50) {
                bestChild = child;
                best = fitness;
            }
        }
        return bestChild;
    }

    public void printMostLikelyEvents() {
        System.err.println("player " + context.getActivePlayerId());
        System.err.println("will " + getBestAction());
        StochasticMCTSNode bestChild = getBestChild();
        if (bestChild != null) {
            double fitness = bestChild.totValue[context.getActivePlayerId()] / (1 + bestChild.nVisits);
            System.err.println("it was visited " + getBestChild().nVisits + " times. Fitness :" + fitness);
            System.err.println();
            if (getBestChild() != null) {
                getBestChild().printMostLikelyEvents();
            }
        }
        return;
    }
    static int numberedRoot = 0;

    public String toDot(int[] parent, int maxDepth, int parentActivePlayer) {

        if (activePlayer == -1) {
            activePlayer = context.getActivePlayerId();
        }
        // if(this.totValue[activePlayer]/this.nVisits > 1.0){
        //    System.err.println("you done bad");
        //    System.exit(0);
        // }
        double newValue = this.totValue[parentActivePlayer] / this.nVisits;
        newValue = Math.round(newValue * 1000);
        newValue /= 1000.0;
        String contrib = parent[0] + " [label=\"" + this.nVisits + ":" + (newValue) + "\", shape=box];\n";
        if (children == null) {
            return contrib;
        }
        if (maxDepth == 0) {
            return contrib;
        }
        int origParent = parent[0];
        for (int i = 0; i < this.children.size(); i++) {
            StochasticMCTSNode child = children.get(i);
            // do the transition here
            parent[0] += 1;
            String action = child.action + "";
            if (child.action instanceof PlayCardAction) {
                action = ((PlayCardAction) child.action).getCardReference().getCardName();
            }
            contrib += origParent + "->" + parent[0] + " [ label=\"" + action + "\"];\n";
            contrib += child.toDot(parent, maxDepth - 1, this.activePlayer);
            //do in that order so we don't have to remember the parent variable we passed in
        }
        return contrib;
    }
}
