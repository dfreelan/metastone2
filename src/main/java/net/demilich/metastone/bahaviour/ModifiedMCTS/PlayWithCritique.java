/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.bahaviour.ModifiedMCTS;

import java.util.ArrayList;
import java.util.List;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.cards.Card;

/**
 *
 * @author dfreelan
 */
public class PlayWithCritique extends Behaviour {

    ArrayList<GameContext> decisionList = new ArrayList<GameContext>();
    ArrayList<Double> decisionScores = new ArrayList<Double>();
    GameCritique critique;

    public PlayWithCritique(GameCritique critique) {
        this.critique = critique.clone();
    }

    @Override
    public String getName() {
        return "play with critique";
    }

    @Override
    public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
        return new ArrayList<Card>();
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {

        int playerID = player.getId();
        double bestScore = -1;
        GameAction bestAction = null;
        GameContext bestContext = null;
        for (GameAction a : validActions) {
            GameContext simulation = context.clone();
            simulation.getLogic().performGameAction(player.getId(), a);
            //System.err.println("raw net output: " + critique.getCritique(simulation,simulation.getPlayer(player.getId())));
            double score = critique.getCritique(simulation,simulation.getPlayer(player.getId()));
            
            //System.err.println("the score for whatever is: " + score);
            if (score > bestScore) {
                bestScore = score;
                bestAction = a;
                bestContext = simulation;
            }
        }
        
        //System.err.println(" i chose " + bestAction);
        decisionList.add(bestContext);
        decisionScores.add(bestScore);

        return bestAction;
        //PlayAllRandomBehavior b = new PlayAllRandomBehavior();
        //return b.requestAction(context, player, validActions);
    }

    @Override
    public IBehaviour clone() {

        PlayWithCritique clone = new PlayWithCritique(critique);
        clone.decisionList = new ArrayList<GameContext>();
        clone.decisionScores = new ArrayList<Double>();

        return clone;
    }

}
