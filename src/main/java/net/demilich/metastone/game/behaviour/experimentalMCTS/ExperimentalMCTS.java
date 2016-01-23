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

    private Random random = new Random();
    private ArrayList<GameAction> turnPlan = null;
    int numTrees;
    int numRollouts;
    double exploreFactor;
    private final boolean deterministic;
    String name = "Experimental MCTS";
    public double[][] results;
    GameContext prevContext;
    GameAction prevAction;
    public ExperimentalMCTS(int numRollouts, int numTrees, double exploreFactor, boolean deterministic) {
        this.numTrees = numTrees;
        this.numRollouts = numRollouts;
        this.exploreFactor = exploreFactor;
        this.deterministic = deterministic;
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

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        this.prevContext = context.clone();
        if (validActions.get(0).getActionType() == ActionType.BATTLECRY) {
            System.err.println("NOPE");
        }
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
        //System.err.println("turn start");

        // double cumulativeWins[] = new double[validActions.size()];
        //double cumulativeTries[] = new double[validActions.size()];
        results = new double[numTrees][validActions.size()];
        IntStream.range(0, numTrees)
                .sequential()//.parallel()
                .forEach((int i) -> getProbabilities(simulation.clone(), validActions, player.clone(), i));
       //for(int i = 0; i<gameForest.length; i++){
        //   getProbabilities(gameForest[i],simulation,validActions, player, i);
        //}

        double totalScore[] = new double[validActions.size()];
        for (int a = 0; a < results.length; a++) {
            for (int i = 0; i < results[a].length; i++) {
                totalScore[i] += results[a][i];
            }
        }
        double bestScore = Double.NEGATIVE_INFINITY;
        GameAction bestAction = validActions.get(0);
        for (int i = 0; i < totalScore.length; i++) {
            if (totalScore[i] > bestScore) {
                bestAction = validActions.get(i);
                bestScore = totalScore[i];
            }
        }
        
        this.prevAction = bestAction.clone();
        System.err.println("ROBOT action was: " + bestAction);
        winner = bestAction;
        return winner;
    }

    public void getProbabilities(GameContext simulation, List<GameAction> validActions, Player player, int index) {
        MCTSTree gameTree;
        System.err.println("index was " + index);
        GameContext temp = simulation.clone();
        temp.getPlayer2().getDeck().shuffle();
        temp.getPlayer1().getDeck().shuffle();
        simulation = simulation.clone();
        Player opponent;
        if (player.getId() == 0) {
            opponent = temp.getPlayer2();
        } else {
            opponent = temp.getPlayer1();
        }
        opponent.getDeck().addAll(opponent.getHand());
        int handSize = opponent.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = opponent.getHand().get(0);
            temp.getLogic().removeCard(opponent.getId(), card);
        }
        opponent.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            temp.getLogic().receiveCard(opponent.getId(), opponent.getDeck().removeFirst());
        }

        gameTree = new MCTSTree(numRollouts / numTrees, validActions, temp, exploreFactor, deterministic);
        if(prevContext != null && prevAction!=null){
            System.err.println("this happens");
          gameTree.root.parentContext = this.prevContext.clone();
          gameTree.root.parentAction = this.prevAction.clone();
          //gameTree.root.action = this.prevAction.clone();
        }
        gameTree.getBestAction();
        MCTSTreeNode root = gameTree.root;
        
        for (int a = 0; a < root.children.size(); a++) {
            MCTSTreeNode child = root.children.get(a);
            System.err.println("a is " + a);
            results[index][a] += child.totValue[player.getId()] / child.nVisits;
        }
        //give the GC something to do.
        root = null;
        gameTree = null;
        return;
    }
}
