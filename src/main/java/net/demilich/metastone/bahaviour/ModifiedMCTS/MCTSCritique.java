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
import net.demilich.metastone.game.behaviour.experimentalMCTS.ExperimentalMCTS;
import net.demilich.metastone.game.behaviour.experimentalMCTS.MCTSSample;
import net.demilich.metastone.game.cards.Card;
import org.encog.engine.network.activation.ActivationElliottSymmetric;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.mathutil.error.ErrorCalculation;
import org.encog.mathutil.error.ErrorCalculationMode;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.ml.train.strategy.Greedy;
import org.encog.ml.train.strategy.HybridStrategy;
import org.encog.ml.train.strategy.StopTrainingStrategy;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.networks.training.anneal.NeuralSimulatedAnnealing;
import org.encog.neural.networks.training.propagation.back.Backpropagation;

/**
 *
 * @author dfreelan
 */
public class MCTSCritique implements GameCritique {

    int samples = 2000;
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
        startingTurn = startingTurn.clone();

        //startingTurn.init();
        startingTurn.getPlayer1().setBehaviour(a.clone());
        startingTurn.getPlayer2().setBehaviour(a.clone());

        ArrayList<double[]> gameFeatures = new ArrayList<double[]>();
        ArrayList<Double> gameLabels = new ArrayList<Double>();

        ArrayList<double[]> gameFeaturesTesting = new ArrayList<double[]>();
        ArrayList<Double> gameLabelsTesting = new ArrayList<Double>();
        
        for (int i = 0; i < samples; i++) {
            System.err.println("on sample " + i);
            GameContext simulation = startingTurn.clone();
            this.RandomizeSimulation(simulation, p);
            ExperimentalMCTS mctsP1 = new ExperimentalMCTS(100, 4, 1.4, false);
            mctsP1.doLog();
            ExperimentalMCTS mctsP2 = new ExperimentalMCTS(100, 4, 1.4, false);
            mctsP2.doLog();
            simulation.getPlayer1().setBehaviour(mctsP1);
            simulation.getPlayer2().setBehaviour(mctsP2);

            simulation.play();

            int result = simulation.getWinningPlayerId();
            for(int q = 0; q<mctsP2.samples.size(); q++){
                MCTSSample sample = mctsP2.samples.get(q);
                System.err.println("there are " + mctsP2.samples.size() + " samples to choose from");
                GameContext randomContext = sample.reachableState;
                double distribution = sample.winRate;
                
                if (i < samples -samples/10) {
                    
                        System.err.println("example of a label: "+  distribution);
                        if(Double.isNaN(distribution)){
                            distribution = 0;
                        }else{
                            distribution = distribution*2-1;
                        }
                        
                        ArrayList<Double> temp = new ArrayList<Double>();
                        double[] info = f.getFeatures(true,randomContext , randomContext.getPlayer2());
                        //f.printFeatures(randomContexts[w], randomContexts[w].getPlayer2());
                        double sum = 0;
                        for(int d = 0; d<info.length; d++){
                            sum+= info[d]*(d+1)*2;
                        }
                        System.err.println("feature vector is all like " + sum + " " + info.length);
                        gameFeatures.add(f.getFeatures(true, randomContext, randomContext.getPlayer2()));
                        gameLabels.add((Double)(double)distribution);
                    
                } else {
                    
                        if(Double.isNaN(distribution)){
                            distribution = 0;
                        }else{
                            distribution = distribution*2-1;
                        }
                        gameFeaturesTesting.add(f.getFeatures(true, randomContext, randomContext.getPlayer2()));
                        gameLabelsTesting.add((Double)(double)distribution);
                    
                }
            }
            
            for(int q = 0; q<mctsP1.samples.size(); q++){
                MCTSSample sample = mctsP1.samples.get(q);
                System.err.println("(1)there are " + mctsP1.samples.size() + " samples to choose from");
                GameContext randomContext = sample.reachableState;
                double distribution = sample.winRate;
                
                if (i < samples -samples/10) {
                    
                        System.err.println("example of a label: "+  distribution);
                        if(Double.isNaN(distribution)){
                            distribution = 0;
                        }else{
                            distribution = distribution*2-1;
                        }
                        
                        ArrayList<Double> temp = new ArrayList<Double>();
                        double[] info = f.getFeatures(true,randomContext , randomContext.getPlayer1());
                        //f.printFeatures(randomContexts[w], randomContexts[w].getPlayer1());
                        double sum = 0;
                        for(int d = 0; d<info.length; d++){
                            sum+= info[d]*(d+1)*2;
                        }
                        System.err.println("feature vector is all like " + sum + " " + info.length);
                        gameFeatures.add(f.getFeatures(true, randomContext, randomContext.getPlayer1()));
                        gameLabels.add((Double)(double)distribution);
                    
                } else {
                    
                        if(Double.isNaN(distribution)){
                            distribution = 0;
                        }else{
                            distribution = distribution*2-1;
                        }
                        gameFeaturesTesting.add(f.getFeatures(true, randomContext, randomContext.getPlayer1()));
                        gameLabelsTesting.add((Double)(double)distribution);
                    
                }
            }

        }

        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, gameFeatures.get(0).length));
//182	182	182	46	27
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 70,.5));
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 70,.5));

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
        //train.iteration(300);
        //train.finishTraining();
        trainNetwork("wat", network, trainingSet, testingSet);
        // if(false) break;
        System.err.println(network.calculateError(trainingSet) + " (training) square error is");
        System.err.println(network.calculateError(testingSet) + " (testing) square error is");

        return this;
    }
    public static double trainNetwork(final String what,
			final BasicNetwork network, final MLDataSet trainingSet, final MLDataSet testingSet) {
		// train the neural network
		CalculateScore score = new TrainingSetScore(trainingSet);
		final MLTrain trainAlt = new NeuralSimulatedAnnealing(
				network, score, 10, 2, 100);
                
                
		final MLTrain trainMain = new Backpropagation(network, trainingSet,.0000001,0.0);

		final StopTrainingStrategy stop = new StopTrainingStrategy();
		trainMain.addStrategy(new Greedy());
		//trainMain.addStrategy(new HybridStrategy(trainAlt));
		trainMain.addStrategy(stop);

		int epoch = 0;
		while (!stop.shouldStop() && epoch<100000) {
			trainMain.iteration();
			System.out.println("Training " + what + ", Epoch #" + epoch
					+ " Error:" + trainMain.getError() );
                        System.err.println(network.calculateError(testingSet) + " (testing) square error is");
                        
			epoch++;
		}
                trainMain.finishTraining();
		return trainMain.getError();
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

        MCTSCritique clone = new MCTSCritique();
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
