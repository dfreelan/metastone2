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

public class PlayAllRandomBehavior extends Behaviour {

	private Random random = new Random();

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
		if (validActions.size() == 1) {
			return validActions.get(0);
		}
		//validActions = context.getValidActions();
		int randomIndex = random.nextInt(validActions.size()-1);
		//we're not going to end turn unless we have to, so if it is, remove end turn from the list and select again
		
		GameAction randomAction = validActions.get(randomIndex);

		
		
		if(!validActions.contains(randomAction)){
			throw new RuntimeException("Ref change random");
		}
		return randomAction;
	}

}
