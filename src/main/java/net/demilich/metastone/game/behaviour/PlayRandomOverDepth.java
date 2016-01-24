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

public class PlayRandomOverDepth extends Behaviour {

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
                int currentTurn = context.getTurn();
                int remainingTurns = MAX_TURNS - currentTurn;
                if(remainingTurns<1)
                    remainingTurns = 1;
                
                boolean printSomething = false;
                
		//validActions = context.getValidActions();
		int randomIndex;
		GameAction randomAction;
                int hash;
                while(validActions.size()>1){
                    randomIndex = random.nextInt(validActions.size()-1);
                    randomAction = validActions.get(randomIndex);
                    
                    if(randomAction.getActionType() == ActionType.SUMMON||randomAction.getActionType() == ActionType.SPELL ){
                        hash = ((PlayCardAction) randomAction).getCardReference().getCardName().hashCode();
                    }else{
                        hash = randomAction.toString().hashCode();
                    }
                    Integer depth = depthLookup.get(hash);
                    
                    if(depth!=null){
                        if(currentTurn >= depth){
                            return randomAction;
                        }
                    }else{
                        int randInt = random.nextInt(remainingTurns);    
                        depthLookup.put(hash,currentTurn+random.nextInt(remainingTurns)+3);
                        if(randInt == 0)
                            return randomAction;
                    }
                    validActions.remove(randomIndex);
                }
                

		return validActions.get(0);
	}

}
