/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.bahaviour.ModifiedMCTS;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.nio.ByteBuffer;
import java.io.*;
import java.net.*;
//import org.
//import org.bytedeco.javacpp.presets.
//import org.bytedeco.javacpp.annotation.
//import org.bytedeco.javacpp.presets.
import org.iq80.leveldb.*;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;
import java.io.*;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
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
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

/**
 *
 * @author dfreelan
 */
public class MCTSCritique implements GameCritique {

    int samples = 10;
    int rollouts = 100;
    int trees = 4;
    double exploreRate= .8;
    
    BasicNetwork network;
    int playerID;
    FeatureCollector f;

    @Override
    public GameCritique trainBasedOnActor(Behaviour a, GameContext startingTurn, Player p) {
        
        Random generator = new Random();
        System.err.println("ellodes");
        f = new FeatureCollector(startingTurn, p);
        f.printFeatures(startingTurn, p);
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
        ArrayList<GameContext> testingSetContexts = new ArrayList<GameContext>();
        ArrayList<Double> testingSetReTests = new ArrayList<Double>();
        for (int i = 0; i < samples; i++) {
            boolean testing = !(i < samples - samples / 10);
            System.err.println("on sample " + i);
            GameContext simulation = startingTurn.clone();
            this.RandomizeSimulation(simulation, p);
            ExperimentalMCTS mctsP1 = new ExperimentalMCTS(rollouts, trees, exploreRate, false);
            mctsP1.doLog();
            ExperimentalMCTS mctsP2 = new ExperimentalMCTS(rollouts, trees, exploreRate, false);
            mctsP2.doLog();
            
            simulation.getPlayer1().setBehaviour(mctsP1);
            simulation.getPlayer2().setBehaviour(mctsP2);

            simulation.play();

            int result = simulation.getWinningPlayerId();
            for (int q = 0; q < mctsP2.samples.size(); q++) {
                MCTSSample sample = mctsP2.samples.get(q);
                System.err.println("there are " + mctsP2.samples.size() + " samples to choose from");
                GameContext randomContext = sample.reachableState;
                double distribution = sample.winRate;

                if (!testing) {

                    System.err.println("example of a label: " + distribution);
                    if (Double.isNaN(distribution)) {
                        distribution = 0;
                    } else {
                        distribution = distribution * 2 - 1;
                    }

                    ArrayList<Double> temp = new ArrayList<Double>();
                    double[] info = f.getFeatures(true, randomContext, randomContext.getPlayer2());
                    //f.printFeatures(randomContexts[w], randomContexts[w].getPlayer2());
                    double sum = 0;
                    for (int d = 0; d < info.length; d++) {
                        sum += info[d] * (d + 1) * 2;
                    }
                    System.err.println("feature vector is all like " + sum + " " + info.length);
                    gameFeatures.add(f.getFeatures(true, randomContext, randomContext.getPlayer2()));
                    gameLabels.add((Double) (double) distribution);

                } else {

                    if (Double.isNaN(distribution)) {
                        distribution = 0;
                    } else {
                        distribution = distribution * 2 - 1;
                    }
                    gameFeaturesTesting.add(f.getFeatures(true, randomContext, randomContext.getPlayer2()));
                    gameLabelsTesting.add((Double) (double) distribution);
                    testingSetReTests.add((Double)sample.testValue*2-1);

                }
            }

            for (int q = 0; q < mctsP1.samples.size(); q++) {
                MCTSSample sample = mctsP1.samples.get(q);
                System.err.println("(1)there are " + mctsP1.samples.size() + " samples to choose from");
                GameContext randomContext = sample.reachableState;
                double distribution = sample.winRate;

                if (i < samples - samples / 10) {

                    System.err.println("example of a label: " + distribution);
                    if (Double.isNaN(distribution)) {
                        distribution = 0;
                    } else {
                        distribution = distribution * 2 - 1;
                    }

                    ArrayList<Double> temp = new ArrayList<Double>();
                    double[] info = f.getFeatures(true, randomContext, randomContext.getPlayer1());
                    //f.printFeatures(randomContexts[w], randomContexts[w].getPlayer1());
                    double sum = 0;
                    for (int d = 0; d < info.length; d++) {
                        sum += info[d] * (d + 1) * 2;
                    }
                    System.err.println("feature vector is all like " + sum + " " + info.length);
                    gameFeatures.add(f.getFeatures(true, randomContext, randomContext.getPlayer1()));
                    gameLabels.add((Double) (double) distribution);

                } else {

                    if (Double.isNaN(distribution)) {
                        distribution = 0.00001;
                    } else {
                        distribution = distribution * 2 - 1;
                    }
                    gameFeaturesTesting.add(f.getFeatures(true, randomContext, randomContext.getPlayer1()));
                    gameLabelsTesting.add((Double) (double) distribution);
                    testingSetReTests.add(sample.testValue*2-1);

                }
            }

        }

        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, gameFeatures.get(0).length));
//182	182	182	46	27
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 40, .5));
        network.addLayer(new BasicLayer(new ActivationElliottSymmetric(), true, 40, .5));
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
        
        sendCaffeData(trainingFeatures, trainingLabels, "HearthstoneTraining.h5", true,20,(int)(gameFeatures.size()*1.6));
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
        sendCaffeData(testingFeatures, testingLabels,"HearthstoneTesting.h5", true);
        NeuralDataSet testingSet = new BasicNeuralDataSet(testingFeatures, testingLabels);
        System.err.println("testing err if just 0's" + this.getErrorIfZero(testingLabels));
        System.err.println("ideal testing err: " + this.getIdealTestingError(testingLabels, testingSetContexts,testingSetReTests));
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

        final MLTrain trainMain = new Backpropagation(network, trainingSet,.000001,0.0);

        final StopTrainingStrategy stop = new StopTrainingStrategy();
       // trainMain.addStrategy(new Greedy());
        //trainMain.addStrategy(new HybridStrategy(trainAlt));
        trainMain.addStrategy(stop);

        int epoch = 0;
        while (!stop.shouldStop() && epoch < 100000) {
            trainMain.iteration();
            System.out.println("Training " + what + ", Epoch #" + epoch
                    + " Error:" + trainMain.getError());
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

    public void saveDataToDB(double[][] data, double[][] labels) throws Exception {
        WriteBatch batch;
        Options options = new Options();
        options.createIfMissing(true);
        new File("HearthstoneData").delete();

        DB db = factory.open(new File("HearthstoneData"), options);

        batch = db.createWriteBatch();

        for (int i = 0; i < data.length; i++) {
            //batch.put(toByteArray(i),);
        }
        /*optional int32 channels = 1;
         optional int32 height = 2;
         optional int32 width = 3;
         // the actual image data, in bytes
         optional bytes data = 4;
         optional int32 label = 5;
         // Optionally, the datum could also hold float data.
         repeated float float_data = 6;
         // If true data contains an encoded image that need to be decoded
         optional bool encoded = 7 [default = false];*/
        db.write(batch);
        batch.close();
        db.close();

    }

    public byte[] toByteArray(float... values) {
        ByteBuffer bbuf = ByteBuffer.allocate(4 * values.length);

        for (float v : values) {
            bbuf.putFloat(v);
        }

        return bbuf.array();
    }

    public byte[] toByteArray(int... values) {
        ByteBuffer bbuf = ByteBuffer.allocate(4 * values.length);

        for (int v : values) {
            bbuf.putInt(v);
        }

        return bbuf.array();
    }

    public byte[] toByteArray(Datum a) {
        byte[] bytes = new byte[4 * 4 + a.data.length + 1];
        ByteBuffer bbuf = ByteBuffer.allocate(4 * 4 + a.data.length + 1);

        bbuf.putInt(a.channels);
        bbuf.putInt(a.height);
        bbuf.putInt(a.width);

        bbuf.put(toByteArray(a.data));

        //bbuf.put(a.label);
        return bytes;

    }
    public void sendCaffeData(double[][] data2, double[][] labels2, String name, boolean ordered, int numItemsInBatch, int numBatches){
        double[][] batchedData = new double[numBatches*numItemsInBatch][];
        double[][] batchedLabels = new double[numBatches*numItemsInBatch][];
        Random generator = new Random();
        
        for(int i = 0; i<batchedData.length; i++){
            if(i%1000 == 0 )
                System.err.println("i is " + i);
            int randomIndex = generator.nextInt(data2.length);
            batchedData[i] = data2[randomIndex];
            batchedLabels[i] = labels2[randomIndex];
        }
        
        sendCaffeData(batchedData,batchedLabels,name,true);
        
        
    }
    public void sendCaffeData(double[][] data2, double[][] labels2, String name, boolean ordered) {
        double[][] data = new double[data2.length][];
        double[][] labels = new double[data2.length][];
        if(!ordered){
            for(int i = 0; i<data2.length; i++){
                data[i] = data2[i].clone();
                labels[i] = labels2[i].clone();
            }
        }else{
            data = data2;
            labels = labels2;
        }
        try {
            
            String sentence;
            String modifiedSentence;
            Random generator = new Random();
            //set up our socket server
            Socket clientSocket = new Socket("localhost", 5003);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToServer.writeBytes(name + "\n");
            outToServer.writeBytes((data.length + " " + data[0].length + " " + labels[0].length) + "\n");
            
            //add in data in a random order
            for (int i = 0; i < data.length; i++) {
                if(i%1000 == 0 )
                    System.err.println("i is " + i);
                String features = "";
                int randomIndex = generator.nextInt(data.length-i);   
            
                if(!ordered){
                    swap(data,i,i+randomIndex);
                    swap(labels,i,i+randomIndex);
                }
                for (int a = 0; a < data[i].length; a++) {
                    features += data[i][a] + " ";
                }
                String myLabels = "";
                for (int a = 0; a < labels[i].length; a++) {
                    myLabels += labels[i][a] + " ";
                }
                System.err.println("writing some bytes");
                outToServer.writeBytes(features +"\n");
                System.err.println("writing some more bytes");
                outToServer.writeBytes(myLabels + "\n");
            }

            outToServer.writeBytes("done\n");
            inFromServer.readLine();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("server wasn't waiting");
        }
        System.err.println("hey i sent somethin!");

    }
    public void swap(double[][] data, int index1, int index2){
        double[] temp = data[index1];
        data[index1] = data[index2];
        data[index2] = temp;
    }
    private double getErrorIfZero(double[][] testingLabels) {
        double sum = 0;
        for(int i = 0; i<testingLabels.length; i++){
            sum += testingLabels[i][0]*testingLabels[i][0];
        }
        return sum/((double)testingLabels.length);
    }
    
    private double getIdealTestingError(double[][] labels, ArrayList<GameContext> games, ArrayList<Double> testValues){
        double sum = 0;
        
        for(int i = 0; i<labels.length; i++){
           if(testValues.size() != labels.length){
               System.err.println("MISMATCH!");
               System.exit(0);
           }
            
            double actualResult = testValues.get(i);

            sum+= ((labels[i][0]-actualResult)*(labels[i][0]-actualResult))/((double)labels.length);
            if(Double.isInfinite((labels[i][0]-actualResult)*(labels[i][0]-actualResult))){
                System.err.println("HEY FUCK UP!! " + labels[i][0]  + " " + actualResult);
            }
            System.err.println("test value was " + labels[i][0] + " actual value " + actualResult + " " + labels.length +  " " + testValues.size());
            
        }
        return sum;
    }

}

class Datum {

    int channels;
    int height;
    int width;
    float[] data;
    int label;
    boolean encoded;

}
