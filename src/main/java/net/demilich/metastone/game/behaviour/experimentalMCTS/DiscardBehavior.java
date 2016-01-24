/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.game.behaviour.experimentalMCTS;

import java.util.List;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.cards.Card;

/**
 *
 * @author dfreelan
 */
class DiscardBehavior implements IBehaviour {
    List<Card> discard; 
    public DiscardBehavior(List<Card> discardedCards) {
        this.discard = discardedCards;
    }

    @Override
    public IBehaviour clone() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
        return discard;
    }

    @Override
    public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
