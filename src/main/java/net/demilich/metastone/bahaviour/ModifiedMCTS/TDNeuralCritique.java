/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.bahaviour.ModifiedMCTS;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.PlayAllRandomBehavior;
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.FeatureCollector;
import net.demilich.metastone.game.cards.Card;
import org.encog.engine.network.activation.ActivationElliottSymmetric;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.mathutil.error.ErrorCalculation;
import org.encog.mathutil.error.ErrorCalculationMode;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;

/**
 *
 * @author dfreelan
 */
public class TDNeuralCritique implements GameCritique {

    int gamesPerGradient = 4;
    int samples = 1000;
    BasicNetwork network;
    int playerID;
    FeatureCollector f;
    ArrayList<Sample>[] sampleArr = new ArrayList[gamesPerGradient];
    
    
    private NeuralDataSet makeDataSetFromArrs() {
        int totalSamples = 0;
        for(int i = 0; i<sampleArr.length; i++){
            totalSamples+=sampleArr[i].size();
        }
        
        double input[][] = new double[totalSamples][f.getFeatures(false, sampleArr[0].get(0).context, sampleArr[0].get(0).context.getActivePlayer()).length];
        double desiredOut[][] = new double[totalSamples][1];
        int count = 0;
        for(int i = 0; i<sampleArr.length; i++){
            for(int a = 0; a<sampleArr[i].size(); a++){
                input[count] = f.getFeatures(true, sampleArr[i].get(a).context, sampleArr[i].get(a).context.getActivePlayer());
                
                double[] arr = {sampleArr[i].get(a).result};
                desiredOut[count] = arr;
            }
        }
        return new BasicNeuralDataSet(input, desiredOut);
    }
    @Override
    public GameCritique trainBasedOnActor(Behaviour a, GameContext startingTurn, Player p) {
        
        NeuralCritique baseline = new NeuralCritique();
        
           startingTurn = startingTurn.clone();
        startingTurn.init();
        f = new FeatureCollector(startingTurn, p);
        System.err.println("before I call it it's " +f.getFeatures(true, startingTurn.clone(), p.clone()).length );
        baseline.trainBasedOnActor(((PlayAllRandomBehavior)a).clone(), startingTurn.clone(), p.clone());
        
        Random generator = new Random();
     
        //f = new FeatureCollector(startingTurn, p);
        
        System.err.println("after num features in TDNeural is : " + f.getFeatures(true, startingTurn, p).length);
        
        playerID = p.getId();
        network = baseline.network;
   /*      network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, f.getFeatures(true, startingTurn.clone(), p.clone()).length));

        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 70));
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 70));
        
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 1));
        network.getStructure().finalizeStructure();
        network.reset();*/
        

        startingTurn.getPlayer1().setBehaviour(new PlayWithCritique(this.clone()));
        startingTurn.getPlayer2().setBehaviour(new PlayWithCritique(this.clone()));
        
        for (int i = 0; i < samples; i++) {
            System.err.println("on sample " + i);
            final GameContext sim = startingTurn.clone();
            IntStream.range(0, gamesPerGradient)
                    .parallel()
                    .forEach((int r) -> {
                        playGame(sim.clone(),r);
            });
            NeuralDataSet data = makeDataSetFromArrs();
            Backpropagation train = new Backpropagation(network,data);
            train.iteration(1);
        }

       
        return this;
    }

    @Override
    public double getCritique(GameContext context, Player p) {

        double[] output = new double[1];
        //System.err.println("network is " + network + " " + context + " " + f);
        network.compute(f.getFeatures(true, context.clone(), p), output);

        return (output[0] + 1.0) / 2.0;
        //NeuralDataSet sample = new BasicNeuralDataSet(new double[][] 
        //{ f.getFeatures(true, context, context.getPlayer(playerID)) }); 

    }

    @Override
    public GameCritique clone() {

        TDNeuralCritique clone = new TDNeuralCritique();
        clone.network = (BasicNetwork) this.network.clone();
        clone.f = this.f.clone();

        return clone;
    }

    public GameContext RandomizeSimulation(GameContext simulation, Player player) {
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

        if (player.getId() == 1) {
            opponent = simulation.getPlayer2();
        } else {
            opponent = simulation.getPlayer1();
        }
        opponent.getDeck().addAll(opponent.getHand());
        handSize = opponent.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = opponent.getHand().get(0);
            simulation.getLogic().removeCard(opponent.getId(), card);
        }
        opponent.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            simulation.getLogic().receiveCard(opponent.getId(), opponent.getDeck().removeFirst());
        }

        return simulation;
    }

    public ArrayList<Sample> playGame(GameContext simulation, int index) {

        ArrayList<Sample> samples = new ArrayList<Sample>();
        sampleArr[index] = samples;
       
        simulation.getPlayer1().setBehaviour(simulation.getPlayer1().getBehaviour().clone());
        simulation.getPlayer2().setBehaviour(simulation.getPlayer2().getBehaviour().clone());
        simulation = this.RandomizeSimulation(simulation, simulation.getPlayer1());
        
        //simulation.init();
        while (!simulation.gameDecided()) {
            simulation.startTurn(simulation.getActivePlayerId());
            simulation.playTurn();
        }
        
        PlayWithCritique p1 = (PlayWithCritique)simulation.getPlayer1().getBehaviour();
        PlayWithCritique p2 = (PlayWithCritique)simulation.getPlayer2().getBehaviour();
        
        double result = simulation.getWinningPlayerId();
        if(result == -1)
            result = .5;
        double p1Result = (1-result)*.02;
        double p2Result = (1-p1Result)*.02;
        for(int i = 0; i<p1.decisionList.size();i++){
            double value = p1Result+p1.decisionScores.get(i)*.980;
            value = value*2 -1;
            samples.add(new Sample(p1.decisionList.get(i),value,0));
            //System.err.println("the result of the game was: " + result);
        }
        for(int i = 0; i<p2.decisionList.size();i++){
            double value = p2Result+p2.decisionScores.get(i)*.980;
            value = value*2 -1;
            samples.add(new Sample(p2.decisionList.get(i),value,1));
        }
        
        double result2 = simulation.getWinningPlayerId();
        if (result2 == -1) {
            result2 = (double) .5;
        }
        result += result2;
        // System.err.println("result is now.. " + result);

        
        return samples;
    }

 

}

class Sample {

    GameContext context;
    double result;
    double player;
    public Sample(GameContext context, double result, double player){
        this.context = context;
        this.result = result;
        this.player = player;
    }
}
