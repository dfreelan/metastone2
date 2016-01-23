package net.demilich.metastone.game.behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.targeting.EntityReference;

public class PlayAllRandomBiasedBehavior extends Behaviour {

    private Random random = new Random();

    public PlayAllRandomBiasedBehavior(String kelsey) {
        random = new Random(kelsey.hashCode());
    }

    public PlayAllRandomBiasedBehavior() {
    }

    @Override
    public String getName() {
        return "Play All Random";
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
        for (int i = 0; i < validActions.size(); i++) {
            GameAction myAction = validActions.get(i);
            if (myAction.getActionType().equals(ActionType.SPELL)) {
                if (random.nextInt(2) == 1) {
                    validActions.remove(i);
                    i--;
                }
            }
        }

        if (validActions.size() == 1) {
            return validActions.get(0);
        }

        int randomIndex = random.nextInt(validActions.size() - 1);

        GameAction randomAction = validActions.get(randomIndex);

        if (randomAction.getActionType().equals(ActionType.PHYSICAL_ATTACK) && randomAction.getTargetKey().equals(EntityReference.ENEMY_HERO)) {
            randomIndex = random.nextInt(validActions.size() - 1);
            randomAction = validActions.get(randomIndex);
        }
        return randomAction;
    }

}
