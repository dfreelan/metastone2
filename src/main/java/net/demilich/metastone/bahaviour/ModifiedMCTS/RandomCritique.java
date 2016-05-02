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
public class RandomCritique implements GameCritique {

    @Override
    public GameCritique trainBasedOnActor(Behaviour a, GameContext startingTurn, Player p) {
        return this;
    }

    @Override
    public double getCritique(GameContext context, Player p) {
        return Math.random();
    }
    
    public GameCritique clone(){
        return new RandomCritique();
    }
    
}
