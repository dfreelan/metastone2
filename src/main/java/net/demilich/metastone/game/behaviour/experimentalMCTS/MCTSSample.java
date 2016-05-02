/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.game.behaviour.experimentalMCTS;

import net.demilich.metastone.game.GameContext;

/**
 *
 * @author dfreelan
 */
public class MCTSSample{
    public GameContext[] reachableStates;
    public double[] winRates;
    public MCTSSample(GameContext g[], double[] w){
        this.reachableStates  =g;
        this.winRates = w;
    }
}
