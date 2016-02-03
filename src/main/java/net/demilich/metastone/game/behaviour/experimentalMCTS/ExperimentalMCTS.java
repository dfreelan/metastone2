package net.demilich.metastone.game.behaviour.experimentalMCTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;
import net.demilich.metastone.game.targeting.CardLocation;
//10000 and 1.0 explore had 56:44 win rate on origional
//explore rate 1.25
//10000/20 vs GSV, won 33/50 games
//10000/40 vs GSV, won 28/30 games
//10000/30 vs GSV won 40/50 games
//explore rate 1.414
//10000/30 won 38/50 games?? experiment may now be invalid
//changing back to 1.25
//got rid of opponent knowledge of my hand.
//
//10000/30,1.25
//7:10
//31:19
//with RNG:
//31:19

//10000/30, 1.25 
//70:30 nobattlecry
//now let's try
//adding in mulltigan + averaging probabilities, not weighted average
//literally same score without weighted average and with mulligan applied
//removed mulligan
//66:33 (2 game difference)
//added in back mulligan, but now >4
//with 60 and 10000 got 33:17
//with 60 and 10000 got 34:16
//60 and 10000, got 35:15
//doing another trial here ^^
//with 30 and 10000 got 31:19:: did a resh build: 35:15--- should probably test again.... but these are really minor differences
//30:20 mulligan >4
//back to mulligan >3
//
public class ExperimentalMCTS extends Behaviour {
    

    int numTrees;
    int numRollouts;
    double exploreFactor;
    public static GameAction lastPlayedAction = null;
    String name = "Experimental MCTS";

    private IGameStateHeuristic heuristic = new ThreatBasedHeuristic(FeatureVector.getDefault());

    public ExperimentalMCTS(int numRollouts, int numTrees, double exploreFactor, boolean deterministic) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
       
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
        List<Card> discardedCards = new ArrayList<Card>();
        for (Card card : cards) {
            if (card.getBaseManaCost() > 4) {
                discardedCards.add(card);
            }
        }
        //make a method, and pass it the cards we got
        //
        return discardedCards;
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        TreeWrapper myTree = new TreeWrapper(numRollouts,numTrees,exploreFactor);
        return myTree.getAction(context,player,validActions);
    }

}

class TreeWrapper {

    public double[][] results;
    int numTrees;
    int numRollouts;
    double exploreFactor;
    
    public TreeWrapper(int numRollouts, int numTrees, double exploreFactor) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
    }
    public GameAction getAction(GameContext context, Player player, List<GameAction> validActions){
        GameContext simulation = context.clone();
        for (int i = 0; i < validActions.size(); i++) {
            GameAction action = validActions.get(i);
            if (action.getActionType().equals(ActionType.SUMMON) && action.getTargetKey() != null) {
                validActions.remove(i);
                i--;
            }
        }
        //simulation.playFromMiddle();
        if (validActions.size() == 1) {
            return validActions.get(0);
        }

        GameAction winner = null;

        results = new double[numTrees][validActions.size()];
        IntStream.range(0, numTrees)
                .sequential()
                .forEach((int i) -> getProbabilities(simulation.clone(), validActions, player.clone(), i));
        //for(int i = 0; i<gameForest.length; i++){
        //   getProbabilities(gameForest[i],simulation,validActions, player, i);
        //}

        double totalScore[] = new double[validActions.size()];
        for (int a = 0; a < results.length; a++) {
            for (int i = 0; i < results[a].length; i++) {
                totalScore[i] += results[a][i] / numTrees;
            }
        }
        double bestScore = Double.NEGATIVE_INFINITY;
        double secondBestScore = Double.NEGATIVE_INFINITY;
        GameAction bestAction = validActions.get(0);
        GameAction secondBestAction = validActions.get(0);
        for (int i = 0; i < totalScore.length; i++) {
            if (totalScore[i] > bestScore) {
                secondBestScore = bestScore;
                secondBestAction = bestAction;
                bestAction = validActions.get(i);
                bestScore = totalScore[i];

            } else if (secondBestScore < totalScore[i]) {
                secondBestAction = validActions.get(i);
                secondBestScore = totalScore[i];
            }
            //System.err.println("action " + validActions.get(i) + " fitness: " + totalScore[i]);
        }

        /*if(bestScore-secondBestScore < .025){
         GameContext bestContext = context.clone();
         bestContext.getLogic().performGameAction(player.getId(), bestAction);
         double bestH = heuristic.getScore(bestContext, player.getId());
            
         GameContext secondBestContext = context.clone();
         secondBestContext.getLogic().performGameAction(player.getId(), secondBestAction);
         double secondBestH = heuristic.getScore(secondBestContext, player.getId());
            
         if(bestH >= secondBestH){
         winner = bestAction;
         }else{
         winner = secondBestAction;
         }
         }else{
         winner = bestAction;
         }*/
        if(bestAction.getActionType()!=ActionType.PHYSICAL_ATTACK){
            ExperimentalMCTS.lastPlayedAction = bestAction;
        }
        winner = bestAction;

        //System.err.println("ROBOT action was: " + bestAction + " turn is "  + context.getTurn());
        return winner;
    }
    public void RandomizeSimulation(GameContext simulation, Player player) {
        simulation.getPlayer2().getDeck().shuffle();
        simulation.getPlayer1().getDeck().shuffle();
        Player opponent;
        if (player.getId() == 0) {
            opponent = simulation.getPlayer2();
        } else {
            opponent = simulation.getPlayer1();
        }
        opponent.getDeck().addAll(opponent.getHand());
        int handSize = opponent.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = opponent.getHand().get(0);
            simulation.getLogic().removeCard(opponent.getId(), card);
        }
        opponent.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            simulation.getLogic().receiveCard(opponent.getId(), opponent.getDeck().removeFirst());
        }
    }

    public void getProbabilities(GameContext simulation, List<GameAction> validActions, Player player, int index) {
        MCTSTree gameTree;

        RandomizeSimulation(simulation, simulation.getPlayer(player.getId()));

        gameTree = new MCTSTree(numRollouts / numTrees, validActions, simulation, exploreFactor, false);
        gameTree.getBestAction();
        //f(index==0){
        //   System.err.println("HEY THIS HAPPEND");

        //}
        MCTSTreeNode root = gameTree.root;

        //accumulate results
        for (int a = 0; a < root.children.size(); a++) {
            MCTSTreeNode child = root.children.get(a);
            results[index][a] += child.totValue[player.getId()] / child.nVisits;
            //if(child.totValue[player.getId()]  == 0){
            //  System.err.println("it was 0, but i visisted it " + child.nVisits + " times");
            //  System.err.println("enemy wins " + child.totValue[player.getId()] + " times");
            //  System.err.println(child.action + " times");
            //}
        }
       // if(results[index][0]>results[index][1]){
        //      gameTree.saveTreeToDot("/home/dfreelan/gametree" + index + ".txt");
        //}
        //give the GC something to do.
        root = null;
        gameTree = null;
        return;
    }
}
