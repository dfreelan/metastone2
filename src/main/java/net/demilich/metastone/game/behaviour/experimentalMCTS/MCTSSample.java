/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.game.behaviour.experimentalMCTS;

import java.util.ArrayList;
import java.util.List;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.actions.GameAction;

/**
 *
 * @author dfreelan
 */
public class MCTSSample{
    public GameContext reachableState;
    public double winRate;
    public List<GameAction> gameActions = new ArrayList<GameAction>();
    public double testValue;
    public MCTSSample(GameContext g, double w, List<GameAction> gameActions, double testValue){
        this.reachableState  = g;
        this.winRate = w;
        this.gameActions = gameActions;
        this.testValue = testValue;
        
    }
}
