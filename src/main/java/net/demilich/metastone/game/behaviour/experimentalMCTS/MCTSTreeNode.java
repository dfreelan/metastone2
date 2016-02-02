package net.demilich.metastone.game.behaviour.experimentalMCTS;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Hashtable;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.actions.PlayCardAction;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.PlayAllRandomBiasedBehavior;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;

/*
 * 
 *  largely inspired from (as copy-pasted then heavily modified)
 *  http://mcts.ai/code/java.html
 *  
 */
public class MCTSTreeNode {

    static Random r = new Random();

    CardCollection preShuffledDeck;

    static double epsilon = 1e-6;

    List<MCTSTreeNode> children = null;
    double nVisits;
    double totValue[] = new double[2];
    GameContext context;
    List<GameAction> actions = null;
    //IGameStateHeuristic h = new ThreatBasedHeuristic(FeatureVector.getDefault());
    double hValue = 0.0;
    int activePlayer = -1;
    int winningPlayer = -1;
    double exploreFactor = 1.0;
    GameAction action;
    private boolean firstEndTurn = false;

    public MCTSTreeNode(GameContext simulation, double exploreFactor, boolean firstEndTurn) {
        this.context = simulation;
        this.exploreFactor = exploreFactor;
        //this.rerollEvents = rerollEvents;
        this.firstEndTurn = firstEndTurn;

    }

    public void selectAction() {
        //context.play();
        List<MCTSTreeNode> visited = new LinkedList<MCTSTreeNode>();
        MCTSTreeNode cur = this;
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
            MCTSTreeNode newNode = cur.select();
            visited.add(newNode);
            value = rollOut(newNode);
            if(value==-1){
              //  System.err.println("uhh value was -1 after rollout: times" );
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
            if(value == -1){
                System.err.println("hey.. uh value is weird times");
            }
        } else {
            System.err.println("times... i guess");
            totValue[0] = Double.NEGATIVE_INFINITY;
            totValue[1] = Double.NEGATIVE_INFINITY;
            value = -1;
        }
       

        for (MCTSTreeNode node : visited) {

            node.updateStats(value);
        }
    }

    public double getCost(GameAction action) {
        
        if(action!=null  && isCard(action))
            return .1/(context.getTurn()/2+1);
        
        return 0.0;
    }

    public boolean isCard(GameAction randomAction) {
        return (randomAction.getActionType() == ActionType.SUMMON || randomAction.getActionType() == ActionType.SPELL || randomAction.getActionType() == ActionType.EQUIP_WEAPON);

    }
    Hashtable<Integer, MCTSTreeNode> childHash = null;

    public int hash(MCTSTreeNode state) {
        return state.context.toString().hashCode();
    }

    public void applyAction() {
        if (action == null) {//this should only really happen for the root node.
            return;
        }

        //this.context = this.context.clone();
        context.getLogic().performGameAction(context.getActivePlayerId(), action);
        if (action.getActionType() == ActionType.END_TURN) {
            context.startTurn(context.getActivePlayerId());
        }
        actions = context.getValidActions();

    }

    public void expand(int playerID) {
        assert (actions != null);
        assert (actions.size() > 0);
        if(actions.size() == 0 ){
            throw new RuntimeException("There were 0 actions supplied after applying action " + action + "in context " + this.context);
        }
        ArrayList<MCTSTreeNode> newNodes = new ArrayList<MCTSTreeNode>(actions.size());

        for (GameAction action : actions) {
            MCTSTreeNode newNode = new MCTSTreeNode(context.clone(), exploreFactor, firstEndTurn);
            //if (action instanceof PlayCardAction) {
            //    if (this.context.getActivePlayer().getMaxMana() > 1 && ((PlayCardAction) action).getCardReference().getCardName().equals("Whirlwind")) {
            //        continue;
            //    }
            //}
            newNode.action = action.clone();
            newNodes.add(newNode);
            //newNode.parentContext = this.preservedContext;
            //if(this.action!=null)
            //newNode.parentAction = this.action.clone();

        }
        children = newNodes;
        this.activePlayer = context.getActivePlayerId();

       
    }

    private MCTSTreeNode select() {
        MCTSTreeNode selected = children.get(0);

        double bestValue = (selected.totValue[activePlayer]) / (selected.nVisits + epsilon)
                + exploreFactor * (Math.sqrt(Math.log(nVisits + 1) / (selected.nVisits + epsilon))) + r.nextDouble() * epsilon;

        for (int i = 1; i < children.size(); i++) {
            MCTSTreeNode c = children.get(i);
            double uctValue = (c.totValue[activePlayer]) / (c.nVisits + epsilon)
                    + exploreFactor * (Math.sqrt(Math.log(nVisits + 1) / (c.nVisits + epsilon))) + r.nextDouble() * epsilon;
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

    public double rollOut(MCTSTreeNode tn) {
        //play a random game of hearthstone...

        GameContext simulation = tn.context.clone();
        simulation.playFromMiddle();

        return simulation.getWinningPlayerId();
    }

    public void updateStats(double value) {
        nVisits++;
        if (value != -1.0) {
            totValue[(int) value] += 1.0 - this.getCost(action);
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
            MCTSTreeNode child = children.get(i);

//System.err.println("CHILD TOT VALLUE " + child.totValue[child.context.getActivePlayerId()] + " " + "best " + best);
            double fitness = child.totValue[activePlayer] / (1 + child.nVisits);
           
            //System.err.println("action is " + actions.get(i) + ": " + fitness + " " + child.nVisits);
            if (fitness > best) {
                bestAction = actions.get(i);
                best = fitness;
            }
        }
        //System.err.println("best action: " + bestAction + " " + best);
        return bestAction;

    }

    public MCTSTreeNode getBestChild() {
        double best = Double.NEGATIVE_INFINITY;
        MCTSTreeNode bestChild = null;
        //if (children.size() == 0) {
        //   throw new RuntimeException("NO CHILDREN");
        //   }
        if (children == null) {
            return null;
        }
        for (int i = 0; i < children.size(); i++) {
            MCTSTreeNode child = children.get(i);
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
        MCTSTreeNode bestChild = getBestChild();
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
            MCTSTreeNode child = children.get(i);
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
