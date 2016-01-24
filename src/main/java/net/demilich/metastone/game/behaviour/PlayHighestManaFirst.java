package net.demilich.metastone.game.behaviour;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.actions.PlayCardAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.targeting.EntityReference;

public class PlayHighestManaFirst extends Behaviour {

	private Random random = new Random();
        private final static int MAX_TURNS = 60;
        Hashtable<Integer,Integer> depthLookup = new Hashtable<Integer,Integer>();
	@Override
	public String getName() {
		return "Play All Random";
	}
        @Override
	public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
            //System.err.println("happens");
            depthLookup.clear();
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
                int highestManaCost = -1;
                for(GameAction action : validActions){
                    if(action.getActionType() == ActionType.SUMMON||action.getActionType() == ActionType.SPELL ){
                        if(action.getActionType() == ActionType.SPELL && player.getMinions().size() <= context.getOpponent(player).getMinions().size() ){
                            continue;
                        }
                        int cost =  context.resolveCardReference(((PlayCardAction)action).getCardReference()).getBaseManaCost();
                        if(highestManaCost > cost){
                            highestManaCost  = cost;
                        }
                    }
                }
                for(int i = 0; i<validActions.size(); i++){
                    GameAction action = validActions.get(i);
                    if(action.getActionType() == ActionType.SUMMON||action.getActionType() == ActionType.SPELL ){
                        if(action.getActionType() == ActionType.SPELL && player.getMinions().size() <= context.getOpponent(player).getMinions().size() ){
                          continue;
                        } 
                        if(context.resolveCardReference(((PlayCardAction)action).getCardReference()).getBaseManaCost()<highestManaCost){
                             validActions.remove(i);
                             i--;
                         }
                     }
                 }
                int randomIndex = random.nextInt(validActions.size()-1);
                GameAction randomAction = validActions.get(randomIndex);
                

		return randomAction;
	}

}
