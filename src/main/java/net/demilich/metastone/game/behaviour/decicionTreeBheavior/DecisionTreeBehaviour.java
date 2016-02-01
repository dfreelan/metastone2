package net.demilich.metastone.game.behaviour.decicionTreeBheavior;

import gnu.trove.map.hash.TIntIntHashMap;
import net.demilich.metastone.game.behaviour.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

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

public class DecisionTreeBehaviour extends Behaviour {

    private Random random = new Random();
    TIntIntHashMap hashToIndex;
    DecisionDataBase features;
    static DecisionDataBase staticKnowledge = null;
    DecisionDataBase knowledge = null;
    static boolean staticInited = false;
    static int bestOf = 1;
    public DecisionTreeBehaviour() {

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

    public static synchronized void initKnowledge(GameContext context, Player player) {
        if (staticInited) {
            return;
        }
        staticKnowledge = new DecisionDataBase(context, player);
        GameContext simulation = context.clone();

        Player me = simulation.getPlayer(player.getId());
        Player opp = simulation.getOpponent(me);

        me.setBehaviour(new KnowledgeCollectionBehavior(staticKnowledge));
        opp.setBehaviour(new PlayRandomBehaviour());

        IntStream.range(0, 10000).parallel().forEach((int i) -> rollout(simulation, 1, simulation.getPlayer(player.getId())));

        staticKnowledge.learn();
        System.err.println("brawl knowledge:");
        if (staticKnowledge.getCardKnowledge("Brawl") != null) {
            //System.err.println(staticKnowledge.getCardKnowledge("Brawl").tree.getRoot().nodeToDot());
            staticKnowledge.getCardKnowledge("Brawl").probMax.printInfo();
        }

        System.err.println("whirwind knowledge:");
        if (staticKnowledge.getCardKnowledge("Whirlwind") != null) {
            //System.err.println(staticKnowledge.getCardKnowledge("Whirlwind").tree.getRoot().nodeToDot());
            staticKnowledge.getCardKnowledge("Whirlwind").probMax.printInfo();
        }

        staticInited = true;
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        //System.err.println("random player behavior");
       
        while (!staticInited) {
             GameContext simulation = context.clone();
            initKnowledge(simulation, simulation.getPlayer(player.getId()));
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        
        if (knowledge == null) {
            knowledge = (DecisionDataBase) staticKnowledge.clone();
        }
        if (validActions.size() == 1) {
            return validActions.get(0);
        }
        int randomIndex = random.nextInt(validActions.size()-1);

        while (true) {
            GameAction randomAction = validActions.get(randomIndex);
            if (isCard(randomAction)){
                double dec = knowledge.getDecision(context, ((PlayCardAction) randomAction).getCardReference().getCardName(), player);
                if (dec <.51) {
                    validActions.remove(randomIndex);
                } else {
                   return randomAction;
                }
            } else {
                return randomAction;
            }
            if (validActions.size() == 1) {
                return validActions.get(0);
            }
            randomIndex = random.nextInt(validActions.size()-1);

        }
        //GameAction randomAction = validActions.get(random.nextInt(validActions.size()));
        //for all spell/minion actions
        //play a bunch of random games
    }
    public boolean isCard(GameAction randomAction){
        return (randomAction.getActionType() == ActionType.SUMMON || randomAction.getActionType() == ActionType.SPELL || randomAction.getActionType() == ActionType.EQUIP_WEAPON);
     
    }
    //I'm assuming you're passing me something that has already been cloned!
    public static void RandomizeSimulation(GameContext simulation, Player player) {
        simulation.getPlayer2().getDeck().shuffle();
        simulation.getPlayer1().getDeck().shuffle();
        Player opponent;
        if (player.getId() == 0) {
            opponent = simulation.getPlayer2();
        } else {
            opponent = simulation.getPlayer1();
        }
        redealHand(simulation, opponent);
    }

    private static void rollout(GameContext world, int iterations, Player player) {
        for (int i = 0; i < iterations; i++) {
            GameContext simulation = world.clone();
            player = simulation.getPlayer(player.getId());
            RandomizeSimulation(simulation, player);
            redealHand(simulation, player);
            //System.err.println("card at 0 is " + simulation.getPlayer(player.getId()).getHand().peekFirst());
            simulation.playFromMiddle();
        }
    }
//Assumes incoming simulation has already been cloned, it's a helper method for the rollout
    private static void redealHand(GameContext simulation, Player player) {
        player = simulation.getPlayer(player.getId());
        player.getDeck().addAll(player.getHand());
        int handSize = player.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = player.getHand().get(0);
            simulation.getLogic().removeCard(player.getId(), card);
        }
        player.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            simulation.getLogic().receiveCard(player.getId(), player.getDeck().removeFirst());
        }
    }

}
