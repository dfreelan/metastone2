package net.demilich.metastone.behaviour.StochasticKnowledgeMCTS;

import net.demilich.metastone.bahaviour.ModifiedMCTS.*;
import net.demilich.metastone.game.behaviour.experimentalMCTS.*;
import java.io.PrintWriter;
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
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.DecisionTreeBehaviour;
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.FeatureCollector;
import net.demilich.metastone.game.targeting.CardReference;

public class MCTSTree {

    private int iterations = 10000;
    public StochasticMCTSNode root;
    private GameContext simulation = null;

    public MCTSTree() {
    }

    public MCTSTree(int numIterations, List<GameAction> rootActions, GameContext context, double exploreFactor, boolean battlecry, CardReference prevCardReference, FeatureCollector f, GameCritique critique) {
        iterations = numIterations;
        this.simulation = context;
        simulation.getPlayer1().setBehaviour(new PlayAllRandomBehavior());
        simulation.getPlayer2().setBehaviour(new PlayAllRandomBehavior());
        //simulation.getOpponent(context.getActivePlayer()).setBehaviour(new PlayRandomBehaviour());
        root = new StochasticMCTSNode(simulation, exploreFactor, battlecry,f.clone(),critique.clone());
        root.actions = rootActions;
        root.beingCalledFromResolve = battlecry;
       
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

    void killTree(StochasticMCTSNode root) {
        root.actions.clear();
        root.actions = null;
        if (root.children != null) {
            for (StochasticMCTSNode node : root.children) {
                killTree(node);
            }
            root.children.clear();
            root.children = null;
        }
    }

    void saveTreeToDot(String gametreetxt) {
        String dotFile = "digraph MCTSTree{";
        int[] refInt = new int[1];
        dotFile += root.toDot(refInt, 4, simulation.getActivePlayerId());
        dotFile += "}";
        try {
            PrintWriter out = new PrintWriter(gametreetxt);
            out.write(dotFile);
            out.close();
        } catch (Exception e) {
            System.err.println("EXCEPTION DONE");
            e.printStackTrace();
        }
    }

}
