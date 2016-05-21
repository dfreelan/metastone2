package net.demilich.metastone.behaviour.StochasticKnowledgeMCTS;

import net.demilich.metastone.bahaviour.ModifiedMCTS.*;
import net.demilich.metastone.game.behaviour.experimentalMCTS.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.actions.PlayCardAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.*;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;
import net.demilich.metastone.game.targeting.CardLocation;
import net.demilich.metastone.game.targeting.CardReference;

//things to try:
//optimistic decision tree (not much different from pessimistic)
//actual naiive bayes method
///more than 5 features
//different cost functions
//tuning cost function by card and/or turn
//hash table for already seen states
//pre-expansion of states/turn
//"statistically better" than best heuristic action
//fix decision tree clone issue (we shouldn't have to clone, going to be a concurrency problem)

//things to fix:
//why does the psychic version no longer work?
//why is a single instance will all sequential streams using more than 100%
//^^maybe just GC
//make my own gameContext with blackjack and hookers
//somehow make it use its own version of GameLogic....
//deck getRandom, currently dones't get random in order to make things deterministic, however breaks random deck functionality


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


//TODO:
//add feature collector to the treewrapper

public class StochasticMCTS extends Behaviour {

    int numTrees;
    int numRollouts;
    double exploreFactor;
    public static GameAction lastPlayedAction = null;
    String name = "Experimental MCTS";
    CardReference prevCardReference;
    GameCritique critique;
    public StochasticMCTS(int numRollouts, int numTrees, double exploreFactor, GameCritique critique ) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
        this.critique = critique;
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
    
    FeatureCollector f = null;
    
    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        
        //System.err.println("our critiquer thinks: " + critique.getCritique(context));
        //System.err.println("an action is being requested of me");
       // System.err.println("here's what the board looks like to me");
        if(f==null){
            f = new FeatureCollector(context,player);
        }
        //f.getFeatures(false, context, player);
        //f.printFeatures(context, player);
        TreeWrapper myTree = new TreeWrapper(numRollouts, numTrees, exploreFactor, prevCardReference, f,critique);
        GameAction prev = myTree.getAction(context.clone(), player.clone(), validActions);
        if (isCard(prev)) {
            prevCardReference = ((PlayCardAction) prev).getCardReference();
        }
        System.err.println("returned an action " + prev + " " + prev.getTargetKey());
        return prev;
    }

    public boolean isCard(GameAction randomAction) {
        return (randomAction.getActionType() == ActionType.SUMMON || randomAction.getActionType() == ActionType.SPELL || randomAction.getActionType() == ActionType.EQUIP_WEAPON);
    }
}

class TreeWrapper {

    public double[][] results;
    int numTrees;
    int numRollouts;
    double exploreFactor;
    CardReference prevCardReference = null;
    private IGameStateHeuristic heuristic = new ThreatBasedHeuristic(FeatureVector.getDefault());
    FeatureCollector f;
    GameCritique critique;
    
    public TreeWrapper(int numRollouts, int numTrees, double exploreFactor, CardReference prevCardReference, FeatureCollector f, GameCritique critique) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
        this.prevCardReference = prevCardReference;
        this.f = f;
        this.critique = critique;
    }

    public GameAction getAction(GameContext context, Player player, List<GameAction> validActions) {
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
        
        
        double totalScore[] = new double[validActions.size()];
        for (int a = 0; a < results.length; a++) {
            for (int i = 0; i < results[a].length; i++) {
                if(results[a][i]!=Integer.MIN_VALUE){
                    System.err.println(results[a][i] + " the results say " + a + " " + i);
                    totalScore[i] += results[a][i] / numTrees;
                    
                }
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
            System.err.println("action " + validActions.get(i) + " fitness: " + totalScore[i]);
        }

        
        if (bestAction.getActionType() != ActionType.PHYSICAL_ATTACK) {
            StochasticMCTS.lastPlayedAction = bestAction;
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

        gameTree = new MCTSTree(numRollouts / numTrees, validActions, simulation, exploreFactor, validActions.get(0).getActionType() == ActionType.BATTLECRY, prevCardReference,f,critique);

        gameTree.getBestAction();
        //f(index==0){
        //   System.err.println("HEY THIS HAPPEND");

        //}
        StochasticMCTSNode root = gameTree.root;

        //accumulate results
        for (int a = 0; a < root.children.size(); a++) {
            StochasticMCTSNode child = root.children.get(a);
            if(child.nVisits != Integer.MAX_VALUE){
                if(child.nVisits == 0)
                    child.nVisits = 1;
                results[index][a] += child.totValue[player.getId()] / child.nVisits;
            }else{
                results[index][a] = Integer.MIN_VALUE;
             }

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
