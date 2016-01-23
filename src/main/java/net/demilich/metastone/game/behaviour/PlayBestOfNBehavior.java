package net.demilich.metastone.game.behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.targeting.EntityReference;

public class PlayBestOfNBehavior extends Behaviour {

    private Random random = new Random();
    int n = 2;
    IGameStateHeuristic h = new ThreatBasedHeuristic(FeatureVector.getDefault());

    public PlayBestOfNBehavior(String kelsey) {
        random = new Random(kelsey.hashCode());
    }

    public PlayBestOfNBehavior(int n) {
        this.n = n;
    }

    @Override
    public String getName() {
        return "Play All BestOfN";
    }

    @Override
    public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
        return new ArrayList<>();
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		//System.err.println("random player behavior");

        //validActions = context.getValidActions();
        //we're not going to end turn unless we have to, so if it is, remove end turn from the list and select again
        if (validActions.size() == 1) {
            return validActions.get(0);
        }
        GameAction bestAction = validActions.get(validActions.size() - 1);
        double bestFitness = h.getScore(context, player.getId());
        validActions.remove(validActions.size() - 1);
        for (int i = 0; i < n && validActions.size() > 0; i++) {

            GameContext simulation = context.clone();
            int randomIndex = random.nextInt(validActions.size());
            simulation.getLogic().performGameAction(player.getId(), validActions.get(randomIndex));
            double fitness = h.getScore(simulation, player.getId());
            if (fitness > bestFitness) {
                bestAction = validActions.get(randomIndex);
                bestFitness = fitness;
            }
            validActions.remove(randomIndex);

        }

        return bestAction;
    }

}
