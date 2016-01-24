package net.demilich.metastone.game.behaviour.experimentalMCTS;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.PlayAllRandomBiasedBehavior;
import net.demilich.metastone.game.behaviour.PlayBestOfNBehavior;
import net.demilich.metastone.game.behaviour.PlayHighestManaFirst;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.PlayRandomOverDepth;

public class MCTSTree {

    private int iterations = 10000;
    public MCTSTreeNode root;
    private GameContext simulation = null;

    public MCTSTree() {
    }

    public MCTSTree(int numIterations, List<GameAction> rootActions, GameContext context, double exploreFactor, boolean deterministic) {
        iterations = numIterations;
        this.simulation = context;
        simulation.getPlayer1().setBehaviour(new PlayHighestManaFirst());
        simulation.getPlayer2().setBehaviour(new PlayHighestManaFirst());
        root = new MCTSTreeNode(simulation, exploreFactor, deterministic);
        root.actions = rootActions;
    }

    public GameAction getBestAction() {
        for (int i = 0; i < iterations; i++) {
            root.selectAction();
        }
        return root.getBestAction();
    }


    void dispose() {
        killTree(root);
    }

    void killTree(MCTSTreeNode root) {
        root.actions.clear();
        root.actions = null;
        if (root.children != null) {
            for (MCTSTreeNode node : root.children) {
                killTree(node);
            }
            root.children.clear();
            root.children = null;
        }
    }

}
