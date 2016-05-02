/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.bahaviour.ModifiedMCTS;

import java.util.ArrayList;
import java.util.Random;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.Behaviour;
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
public class NeuralCritique implements GameCritique {

    int samples = 10000;
    BasicNetwork network;
    int playerID;
    FeatureCollector f;

    @Override
    public GameCritique trainBasedOnActor(Behaviour a, GameContext startingTurn, Player p) {
        Random generator = new Random();
        System.err.println("ellodes");
        f = new FeatureCollector(startingTurn, p);
        System.err.println("num features in Neural is : " + f.getFeatures(true, startingTurn, p).length);
        playerID = p.getId();
        startingTurn.getPlayer1().setBehaviour(a.clone());
        startingTurn.getPlayer2().setBehaviour(a.clone());

        ArrayList<double[]> gameFeatures = new ArrayList<double[]>();
        ArrayList<Double> gameLabels = new ArrayList<Double>();

        ArrayList<double[]> gameFeaturesTesting = new ArrayList<double[]>();
        ArrayList<Double> gameLabelsTesting = new ArrayList<Double>();

        for (int i = 0; i < samples; i++) {
            //System.err.println("on game " + i);
            GameContext simulation = startingTurn.clone();
            simulation.init();
            RandomizeSimulation(simulation, simulation.getPlayer(p.getId()));

            GameContext exampleState = null;
            ArrayList<GameContext> gameHistory = new ArrayList<GameContext>();

            while (!simulation.gameDecided()) {
                gameHistory.add(simulation.clone());
                simulation.startTurn(simulation.getActivePlayerId());
                simulation.playTurn();

            }
            double result = simulation.getWinningPlayerId();
            if (result == -1) {
                result = (double) .5;
            }

            if (i < samples - 1000) {
                GameContext newStart = gameHistory.get(generator.nextInt(gameHistory.size()));
                gameFeatures.add(f.getFeatures(true, newStart, newStart.getActivePlayer()));
                gameFeatures.add(f.getFeatures(true, newStart, newStart.getOpponent(newStart.getActivePlayer())));
                
                
                int playerPerspective = newStart.getActivePlayer().getId();
                //gameFeatures.add();
                for (int q = 0; q < 10; q++) {
                    simulation = newStart.clone();
                    while (!simulation.gameDecided()) {
                        gameHistory.add(simulation.clone());
                        simulation.startTurn(simulation.getActivePlayerId());
                        simulation.playTurn();

                    }
                    double result2 = simulation.getWinningPlayerId();
                    if (result2 == -1) {
                        result2 = (double) .5;
                    }
                    result += result2;
                    //System.err.println("result is now.. " + result);
                }
                result /= 11.0;
                
                if(playerPerspective ==0){
                    result = 1-result;
                }
                gameLabels.add((double) result * 2 - 1);
                gameLabels.add((double) (1-result) * 2 - 1);
               
               
            } else {

                GameContext newStart = gameHistory.get(generator.nextInt(gameHistory.size()));
                gameFeaturesTesting.add(f.getFeatures(true, newStart, newStart.getActivePlayer()));
                gameFeaturesTesting.add(f.getFeatures(true, newStart, newStart.getOpponent(newStart.getActivePlayer())));
                
                int playerPerspective = newStart.getActivePlayer().getId();
                //gameFeatures.add();
                for (int q = 0; q < 10; q++) {
                    simulation = newStart.clone();
                    while (!simulation.gameDecided()) {
                        gameHistory.add(simulation.clone());
                        simulation.startTurn(simulation.getActivePlayerId());
                        simulation.playTurn();

                    }
                    double result2 = simulation.getWinningPlayerId();
                    if (result2 == -1) {
                        result2 = (double) .5;
                    }
                    result += result2;
                    // System.err.println("result is now.. " + result);
                }
                result /= 11.0;
                if(playerPerspective ==0){
                    result = 1-result;
                }
                gameLabelsTesting.add((double) result * 2 - 1);
                gameLabelsTesting.add((double) (1-result) * 2 - 1);
                
            }
            //System.err.println("label here is: " + (result*2-1));
        }

        /*public BasicLayer(final ActivationFunction activationFunction,
         final boolean hasBias, final int neuronCount, double dropoutRate) {*/
        //while(true){
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, gameFeatures.get(0).length));

        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 70));
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 70));

        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 1));
        network.getStructure().finalizeStructure();
        network.reset();

        double[][] trainingFeatures = new double[gameFeatures.size()][gameFeatures.get(0).length];
        double[][] trainingLabels = new double[gameLabels.size()][1];
        for (int i = 0; i < trainingFeatures.length; i++) {
            trainingFeatures[i] = gameFeatures.get(i);
            double[] arr1 = new double[1];
            arr1[0] = gameLabels.get(i);
            trainingLabels[i] = arr1;
        }

        NeuralDataSet trainingSet = new BasicNeuralDataSet(trainingFeatures, trainingLabels);
        network.setProperty(null, null);
        Backpropagation train = new Backpropagation(network, trainingSet);

        // train.iteration(1000);
        double[][] testingFeatures = new double[gameFeaturesTesting.size()][gameFeaturesTesting.get(0).length];
        double[][] testingLabels = new double[gameLabelsTesting.size()][1];
        for (int i = 0; i < testingFeatures.length; i++) {
            testingFeatures[i] = gameFeaturesTesting.get(i);
            double[] arr1 = new double[1];
            arr1[0] = gameLabelsTesting.get(i);
            testingLabels[i] = arr1;
        }
        NeuralDataSet testingSet = new BasicNeuralDataSet(testingFeatures, testingLabels);

        ErrorCalculation.setMode(ErrorCalculationMode.MSE);
        train.iteration(150);
        train.finishTraining();
        // if(false) break;
        System.err.println(network.calculateError(trainingSet) + " (training) square error is");
        System.err.println(network.calculateError(testingSet) + " (testing) square error is");

        double sum = 0.0;
        for (int i = 0; i < testingFeatures.length; i++) {
            double[] output = new double[1];
            network.compute(testingFeatures[i], output);
            if (output[0] < 0) {
                sum++;
            }
        }
        System.err.println("outputaverage: " + sum / testingFeatures.length);
        // System.err.println(numNeurons[0] + " " + numNeurons[1] + " " + numNeurons[2] + " " + numNeurons[3] + " " + numNeurons[4] + " " + network.calculateError(trainingSet) + " " + network.calculateError(testingSet));
        // }
        return this;
    }

    
    //probability 0-1 of winning from the player erspective
    @Override
    public double getCritique(GameContext context, Player p) {

        double[] output = new double[1];
        //System.err.println("network is " + network + " " + context + " " + f);
        network.compute(f.getFeatures(true, context, p), output);
        
        return (output[0] + 1.0) / 2.0;
        //NeuralDataSet sample = new BasicNeuralDataSet(new double[][] 
        //{ f.getFeatures(true, context, context.getPlayer(playerID)) }); 

    }

    @Override
    public GameCritique clone() {

        NeuralCritique clone = new NeuralCritique();
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

}
