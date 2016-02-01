package net.demilich.metastone.game.behaviour.decicionTreeBheavior;

import gnu.trove.map.hash.TIntIntHashMap;
import net.demilich.metastone.game.behaviour.*;
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
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.targeting.EntityReference;
import sim.app.horde.classifiers.Domain;
import sim.app.horde.classifiers.Example;
import sim.app.horde.classifiers.decisiontree.DecisionTree;

public class KnowledgeCollectionBehavior extends Behaviour {

    private Random random = new Random();
    DecisionDataBase defaultKnowledge = null;
    private int stopTime;
    public KnowledgeCollectionBehavior(DecisionDataBase knowledge) {
        defaultKnowledge = knowledge;
    }
    public void setStopTime(int stop){
        this.stopTime = stop;
        
    }
    public int getStopTime(){
        return stopTime;
    }
    // MersenneTwisterFast rand = new MersenneTwisterFast();
    @Override
    public String getName() {
        return "DecisionTreeBehavior";
    }

    @Override
    public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
        //System.err.println("happens");
    }

    @Override
    public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
        return new ArrayList<>();
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        //System.err.println("random player behavior");
        
        if(validActions.size() == 1){
            return validActions.get(0);
        }
        int randomIndex = random.nextInt(validActions.size());
        if(player.getMana() < 4){
            
            return validActions.get(validActions.size()-1);
        }
        GameAction randomAction = validActions.get(randomIndex);

        if ( randomAction.getActionType() == ActionType.SUMMON || randomAction.getActionType() == ActionType.SPELL || randomAction.getActionType() == ActionType.EQUIP_WEAPON) {
            PlayCardAction myCardAction = (PlayCardAction) randomAction;
            String cardName = myCardAction.getCardReference().getCardName();
            GameContext simulation = context.clone();
            RandomizeSimulation(simulation,simulation.getPlayer(player.getId()));
            GameContext worldWithAction = simulation.clone();

            worldWithAction.getLogic().performGameAction(player.getId(), randomAction);

            GameContext worldWithoutAction = simulation.clone();
            removeCard(worldWithoutAction.getPlayer(player.getId()), cardName);

        
            //world with, perform the action and rollout from there on
            if (!worldWithAction.gameDecided()) {
                
                double winsWithAction = rollout(worldWithAction, 1, player.getId());
                //world without, need to remove the action, select a new one, and remove the card from the hand
                double winsWithoutAction = rollout(worldWithoutAction, 1, player.getId());

                double difference = winsWithAction - winsWithoutAction;
                // if(cardName.equals("Brawl")){
                //    System.err.println("Brawl: dif was "+ difference);
                // }
               
               // if(Math.abs(difference) > 0){
                    if (difference < 0) { //without action was a lot better
                            defaultKnowledge.addExample(context, cardName, player, 0);
                    } else if (difference>0){
                            defaultKnowledge.addExample(context, cardName, player, 2);
                    }
                //}

            }

        }

        return randomAction;
        //GameAction randomAction = validActions.get(random.nextInt(validActions.size()));
        //for all spell/minion actions
        //play a bunch of random games
    }

    //I'm assuming you're passing me something that has already been cloned!
    public void RandomizeSimulation(GameContext simulation, Player player) {
        simulation.getPlayer2().getDeck().shuffle();
        simulation.getPlayer1().getDeck().shuffle();
        Player opponent;
        if (player.getId() == 0) {
            opponent = simulation.getPlayer2();
        } else {
            opponent = simulation.getPlayer1();
        }
        opponent.getDeck().addAll(opponent.getHand());
        int handSize = opponent.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = opponent.getHand().get(0);
            simulation.getLogic().removeCard(opponent.getId(), card);
        }
        opponent.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            simulation.getLogic().receiveCard(opponent.getId(), opponent.getDeck().removeFirst());
        }
    }

    private double rollout(GameContext world, int iterations, int id) {
        double count = 0;
        world.getPlayer1().setBehaviour(new PlayAllRandomBehavior());
        world.getPlayer2().setBehaviour(new PlayAllRandomBehavior());

        for (int i = 0; i < iterations; i++) {
            GameContext simulation = world.clone();
            simulation.playFromMiddle();
            if (id == simulation.getWinningPlayerId()) {
                count++;
            }
        }

        return count;
    }

    private void removeCard(Player player, String cardName) {
        for (int i = 0; i < player.getHand().getCount(); i++) {
            Card card = player.getHand().get(i);
            if (card.getName().equals(cardName)) {
                player.getHand().remove(card);
                i--;
            }
        }
    }
}
