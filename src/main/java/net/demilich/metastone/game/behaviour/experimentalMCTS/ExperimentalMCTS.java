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
        return new ArrayList<>();
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
            //temp.getPlayer2().getDeck().shuffle();
            //temp.getPlayer1().getDeck().shuffle();
            gameForest[i] = new MCTSTree(numRollouts / numTrees, validActions, temp, exploreFactor, deterministic);
            gameForest[i].getBestAction();
            MCTSTreeNode root = gameForest[i].root;
            for (int a = 0; a < root.children.size(); a++) {
                MCTSTreeNode child = root.children.get(a);
                cumulativeWins[a] += child.totValue[context.getActivePlayerId()];
                cumulativeTries[a] += child.nVisits;
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
        //System.err.println("best action was: " + bestAction + " " + bestScore); 
        winner = bestAction;
        return winner;
    }
}
