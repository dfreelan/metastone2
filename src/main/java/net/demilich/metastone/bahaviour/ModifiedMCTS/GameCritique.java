/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.bahaviour.ModifiedMCTS;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.Behaviour;

/**
 *
 * @author dfreelan
 */
public interface GameCritique{
    
    public GameCritique trainBasedOnActor(Behaviour a, GameContext startingTurn, Player p);
    public double getCritique(GameContext context, Player p);//should produce a number between 0 and 1, evaluating current players odds of winning
    public GameCritique clone();
}
