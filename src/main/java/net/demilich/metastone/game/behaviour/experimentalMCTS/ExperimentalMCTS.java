package net.demilich.metastone.game.behaviour.experimentalMCTS;

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
public class ExperimentalMCTS extends Behaviour {

    static {
        System.err.println("cleaned AGAIN BARRYLECRY AGAIN clonedtrying to fix battlcryissue randomization FASTER, CORRECT");    
                    
    }
    int numTrees;
    int numRollouts;
    double exploreFactor;
    public static GameAction lastPlayedAction = null;
    String name = "Experimental MCTS";
    CardReference prevCardReference;
    public boolean logging = false;
    public static boolean interestingSimulation = false;
    public ArrayList<MCTSSample> samples = new ArrayList<MCTSSample>();
    private boolean getTestValues = false;
    FeatureCollector f;

    public void doLog(FeatureCollector f) {
        logging = true;
        TreeWrapper.logging = true;
        this.f = f;
    }

    public void getTestValues() {
        getTestValues = true;
    }

    public ExperimentalMCTS(int numRollouts, int numTrees, double exploreFactor, boolean deterministic, boolean interesting) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
        ExperimentalMCTS.interestingSimulation = interesting;
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
        return discardedCards;
    }

    ArrayList<GameAction> preservedActions;

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        context = context.clone();
        //System.err.println("an action is being requested of me");
        //System.err.println("here's what the board looks like to me");
        if (f == null) {
            f = new FeatureCollector(context, player);
        }
        //f.getFeatures(false, context, player);
        //f.printFeatures(context, player);
        TreeWrapper myTree = new TreeWrapper(numRollouts, numTrees, exploreFactor, prevCardReference);

        GameAction bestAction = myTree.getAction(context.clone(), player.clone(), validActions);

        if (myTree.bestActionScore != -100 && logging) {
            double testValue = 0.0;
            if (myTree.averageRootScore < 0) {
                System.err.println("BAD ROOT SCORE!");
                System.exit(0);
            }
            if (this.getTestValues) {
                TreeWrapper myTreeTester = new TreeWrapper(numRollouts * 10, numTrees * 10, exploreFactor, prevCardReference);
                myTreeTester.getAction(context.clone(), player.clone(), validActions);
                testValue = myTreeTester.bestActionScore;
            }
            samples.add(new MCTSSample(context.clone(), myTree.bestActionScore, preservedActions, testValue, myTree.root, f));

        }
        return bestAction;
    }

    public boolean isCard(GameAction randomAction) {
        return (randomAction.getActionType() == ActionType.SUMMON || randomAction.getActionType() == ActionType.SPELL || randomAction.getActionType() == ActionType.EQUIP_WEAPON);
    }
}

class TreeWrapper {

    static boolean logging = false;
    public double[][] results;
    int numTrees;
    int numRollouts;
    double exploreFactor;
    CardReference prevCardReference = null;
    GameContext[] childContexts = null;
    MCTSSample sample = null;
    double totalScore = 0.0;
    double[] rootScores = null;
    double averageRootScore = -100.0;
    private IGameStateHeuristic heuristic = new ThreatBasedHeuristic(FeatureVector.getDefault());
    ArrayList<MCTSSample> samples = new ArrayList<MCTSSample>();
    double bestActionScore = -100;
    MCTSTreeNode root;

    public TreeWrapper(int numRollouts, int numTrees, double exploreFactor, CardReference prevCardReference) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
        this.prevCardReference = prevCardReference;
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
        this.rootScores = new double[numTrees];
        //System.err.println("root scores length is " + rootScores.length);

        IntStream.range(0, numTrees)
                .parallel()
                .forEach((int i) -> getProbabilities(simulation.clone(), validActions, player.clone(), i));
        //for(int i = 0; i<gameForest.length; i++){
        //   getProbabilities(gameForest[i],simulation,validActions, player, i);
        //}
        averageRootScore = 0.0;
        for (int i = 0; i < rootScores.length; i++) {
            averageRootScore += rootScores[i];
            if (rootScores[i] < 0) {
                System.err.println("root score was " + rootScores[i]);
                System.exit(0);
            }
        }
        averageRootScore /= (double) rootScores.length;

        double totalScore[] = new double[validActions.size()];
        for (int a = 0; a < results.length; a++) {
            for (int i = 0; i < results[a].length; i++) {
                if (results[a][i] != Integer.MIN_VALUE) {
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
            //System.err.println(10E2);
            //System.err.println("competition potaction " + validActions.get(i) + " fitness: " + totalScore[i]);
        }

        this.bestActionScore = bestScore;
        /*if (bestScore - secondBestScore < .025) {
         GameContext bestContext = context.clone();
         bestContext.getLogic().performGameAction(player.getId(), bestAction);
         double bestH = heuristic.getScore(bestContext, player.getId());

         GameContext secondBestContext = context.clone();
         secondBestContext.getLogic().performGameAction(player.getId(), secondBestAction);
         double secondBestH = heuristic.getScore(secondBestContext, player.getId());

         if (bestH >= secondBestH) {
         winner = bestAction;
         } else {
         winner = secondBestAction;
         }
         } else {
         winner = bestAction;
         }*/
        if (bestAction.getActionType() != ActionType.PHYSICAL_ATTACK) {
            ExperimentalMCTS.lastPlayedAction = bestAction;
        }
        winner = bestAction;

         System.err.println("competition action was: " + bestAction + " turn is "  + context.getTurn());
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

    public void getGoodRandomization(GameContext sim, Player player, List<GameAction> validActions) {
        
        GameContext simulation = sim.clone();
        setRandomPlayer(simulation);
        if (validActions.get(0).getActionType() == ActionType.BATTLECRY ||validActions.get(1).getActionType() == ActionType.BATTLECRY ) {
            RandomizeSimulation(simulation, simulation.getPlayer(player.getId()));
            return;
        }
        int result1 = -1;
        int result2 = -1;
        int threshold = (this.numRollouts/this.numTrees / 20)/2;
        GameContext doNothingWorld ;
        GameContext doBestThingWorld;
        GameAction bestAction = getBestAction(simulation.clone(), player, validActions);
        if(bestAction.getActionType() == ActionType.BATTLECRY){
            System.err.println("SLDKFJSLDFJK");
            System.exit(0);
        }
       // setRandomPlayer(doBestThingWorld);
        while (result1 == result2 && threshold > 0) {
            threshold--;
            RandomizeSimulation(simulation, simulation.getPlayer(player.getId()));
            doNothingWorld = simulation.clone();
            doBestThingWorld = simulation.clone();
            setRandomPlayer(doNothingWorld);
            setRandomPlayer(doBestThingWorld);
            doBestThingWorld.getLogic().performGameAction(player.getId(), bestAction);
            doNothingWorld.getLogic().performGameAction(player.getId(), validActions.get(validActions.size() - 1));
           

            doNothingWorld.playFromMiddle();
            doBestThingWorld.playFromMiddle();

            result1 = doNothingWorld.getWinningPlayerId();
            result2 = doBestThingWorld.getWinningPlayerId();
        }
        
        sim.getPlayer1().getDeck().removeAll();
        sim.getPlayer2().getDeck().removeAll();
        sim.getPlayer1().getDeck().addAll(simulation.getPlayer1().getDeck());
        sim.getPlayer2().getDeck().addAll(simulation.getPlayer2().getDeck());
        Player opponent = sim.getOpponent(player);
        int handSize = opponent.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = opponent.getHand().get(0);
            sim.getLogic().removeCard(opponent.getId(), card);
        }
        for (int k = 0; k < handSize; k++) {
            Card card = simulation.getPlayer(opponent.getId()).getHand().get(k);
            sim.getLogic().receiveCard(opponent.getId(), card);
        }
        
            
    }

    public void setRandomPlayer(GameContext simulation) {
        simulation.getPlayer1().setBehaviour(new PlayAllRandomBehavior());
        simulation.getPlayer2().setBehaviour(new PlayAllRandomBehavior());
    }

    public GameAction getBestAction(GameContext simulation, Player player, List<GameAction> validActions) {
        player = simulation.getPlayer(player.getId());
        if (validActions.size() == 1) {
            return validActions.get(0);
        }
        GameAction bestAction = validActions.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (GameAction gameAction : validActions) {
            GameContext simulationResult = simulateAction(simulation.clone(), player.clone(), gameAction);
            double gameStateScore = heuristic.getScore(simulationResult, player.getId());

            if (gameStateScore > bestScore) {
                bestScore = gameStateScore;
                bestAction = gameAction;
                //logger.debug("BEST ACTION SO FAR id:{}", bestAction.hashCode());
            }
            simulationResult.dispose();

        }

        return bestAction;
    }

    public GameContext simulateAction(GameContext context, Player player, GameAction action) {
        context.getLogic().performGameAction(player.getId(), action);
        return context;
    }

    public void getProbabilities(GameContext simulation, List<GameAction> validActions, Player player, int index) {
        MCTSTree gameTree;
        simulation = simulation.clone();
        if(!ExperimentalMCTS.interestingSimulation || validActions.get(0).getActionType() == ActionType.BATTLECRY){
            RandomizeSimulation(simulation, simulation.getPlayer(player.getId()));
        }else{
            this.getGoodRandomization(simulation, player, validActions);
        }
        gameTree = new MCTSTree(numRollouts / numTrees, validActions, simulation, exploreFactor, validActions.get(0).getActionType() == ActionType.BATTLECRY, prevCardReference);

        gameTree.getBestAction();
        //f(index==0){
        //   System.err.println("HEY THIS HAPPEND");

        //}
        MCTSTreeNode root = gameTree.root;
        if (index == 0);
        this.root = root;
        if (index == 0 && logging) {
            childContexts = new GameContext[root.children.size()];
        }
        //accumulate results
        if (root.totValue[player.getId()] == Double.NEGATIVE_INFINITY) {
            root.totValue[player.getId()] = 0;
        } else if (root.totValue[player.getId()] == Double.POSITIVE_INFINITY) {
            root.totValue[player.getId()] = root.nVisits;
        }
        rootScores[index] = root.totValue[player.getId()] / root.nVisits;

        for (int a = 0; a < root.children.size(); a++) {

            MCTSTreeNode child = root.children.get(a);

            if (index == 0 && logging) {
                childContexts[a] = child.context;
            }

            if (child.nVisits != Integer.MAX_VALUE && child.nVisits > 0) {
                results[index][a] += child.totValue[player.getId()] / child.nVisits;
                //System.err.println("root visits: " + root.nVisits + " value " + root.totValue[player.getId()]);

            } else {
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
