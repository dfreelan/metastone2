package net.demilich.metastone.game.behaviour.experimentalMCTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        MCTSTree gameForest[] = new MCTSTree[numTrees];

        double cumulativeWins[] = new double[validActions.size()];
        double cumulativeTries[] = new double[validActions.size()];
        for (int i = 0; i < gameForest.length; i++) {
            GameContext temp = simulation.clone();
            temp.getPlayer2().getDeck().shuffle();
            temp.getPlayer1().getDeck().shuffle();
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

            gameForest[i] = new MCTSTree(numRollouts / numTrees, validActions, temp, exploreFactor, deterministic);

            gameForest[i].getBestAction();
            MCTSTreeNode root = gameForest[i].root;
            for (int a = 0; a < root.children.size(); a++) {
                MCTSTreeNode child = root.children.get(a);
                cumulativeWins[a] += child.totValue[context.getActivePlayerId()]/child.nVisits;
                cumulativeTries[a] += 1;
            }
            //give the GC something to do.
            root = null;
            gameForest[i] = null;

        }

        double bestScore = Double.NEGATIVE_INFINITY;
        GameAction bestAction = validActions.get(0);
        for (int i = 0; i < cumulativeWins.length; i++) {
            double fitness = cumulativeWins[i] / cumulativeTries[i];
            if (bestScore < fitness) {
                bestScore = fitness;

                bestAction = validActions.get(i);
            }
        }
        System.err.println("ROBOT action was: " + bestAction ); 
        winner = bestAction;
        return winner;
    }
}
