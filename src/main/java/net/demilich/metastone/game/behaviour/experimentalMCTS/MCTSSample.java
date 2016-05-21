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
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.FeatureCollector;

/**
 *
 * @author dfreelan
 */
public class MCTSSample{
    public GameContext reachableState;
    public double winRate;
    public List<GameAction> gameActions = new ArrayList<GameAction>();
    public double testValue;
    public double weight = 0.0;
    MCTSTreeNode root;
    int playerID = -1;
    int index = 0;
    double factor = -1;
    MCTSSample[] preExpanded;
    double[] features;
    FeatureCollector f;
    public MCTSSample(GameContext g, double w, List<GameAction> gameActions, double testValue, MCTSTreeNode root, FeatureCollector f){
        this.reachableState  = g;
        this.winRate = w;
        this.gameActions = gameActions;
        this.testValue = testValue;
        this.root = root;
        this.features = f.getFeatures(false, g, g.getPlayer(root.activePlayer));
        this.f = f;
        if(this.root!=null){
            playerID = root.activePlayer;
            preExpanded = expandSample();
        }
        this.root = null;
         index = 0;
    }
      public MCTSSample(int player, GameContext g, double w, List<GameAction> gameActions, double testValue, MCTSTreeNode root, FeatureCollector f){
        this.reachableState  = g;
        this.winRate = w;
        this.gameActions = gameActions;
        this.testValue = testValue;
        this.root = root; 
        this.features = f.getFeatures(false, g, g.getPlayer(player));
        
        this.reachableState = null;
        this.root = null;
        index = 0;
    }
    
    public MCTSSample[] expandSample(){
        if(preExpanded != null)
            return preExpanded;
        double totalVisits = root.nVisits;
        MCTSSample[] expanded = new MCTSSample[(int)totalVisits];
        index = 0;
        factor = 1.0/((double)root.nVisits);
        expandSample(expanded,root);
        //System.err.println("last index was " + index);
        
        if(index<expanded.length){
            MCTSSample[] expanded2 = new MCTSSample[index];
            for(int i = 0; i<expanded2.length; i++){
                expanded2[i] = expanded[i];
            }
            expanded = expanded2;
        }
        return expanded;
    }
    
    public void expandSample(MCTSSample[] expanded, MCTSTreeNode current){
        if(current == null)
            return;
        if(current.activePlayer == -1)
            return;
        if(current.nVisits == 0)
            return;
        
        expanded[index] = new MCTSSample(playerID,current.context,current.totValue[playerID]/current.nVisits,null,0,null,f);
        expanded[index].weight = current.nVisits*factor;
        index++;
       
        if(current.children !=null)
        for(int i = 0; i<current.children.size(); i++){
            expandSample(expanded,current.children.get(i));
        }
        
        return;
        
    }
    public double[] getFeatures(){
        return this.features;
    }
   
}
