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
import java.util.logging.Level;
import java.util.stream.IntStream;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.decicionTreeBheavior.FeatureCollector;
import net.demilich.metastone.game.behaviour.experimentalMCTS.ExperimentalMCTS;
import net.demilich.metastone.game.behaviour.experimentalMCTS.MCTSSample;
import net.demilich.metastone.game.cards.Card;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.caffe;
import org.bytedeco.javacpp.caffe.Caffe;
import org.bytedeco.javacpp.caffe.FloatBlob;
import org.bytedeco.javacpp.caffe.FloatNet;
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
import static org.bytedeco.javacpp.caffe.*;

/**
 *
 * @author dfreelan
 */
public class MCTSCritique implements GameCritique {

    int samples = 11;
    int rollouts = 400;
    int trees = 1;
    double exploreRate = 1.4;

    BasicNetwork network;
    int playerID;
    FeatureCollector f;
    static FloatNet caffe_net = null;

    String modelFile = null;

    static {
        java.util.logging.Logger.getLogger(caffe.class.getSimpleName()).setLevel(Level.OFF);;
        System.err.println("changing to ");
        System.err.println("AAAAAAAAAAAAAAAAAAAAAAAH\n\n\n\n\n");

        System.err.println("rollout fix changing to .1 neural and .9 playthroughfoeced clone. 10000 samples. 4 rollou555ts executing updated");
        System.err.println("correcte\n\\nd");
        System.err.println("updated");
    }

    public MCTSCritique(String modelFile) {
        //System.err.println("someething new requested of me");
        //caffe_net.

        if (caffe_net != null) {
            return;
        }

        this.modelFile = modelFile;
        modelFile = null;
        modelFile = "/home/dfreelan/dev/networks/hybrid.caffemodel";
        String paramFile = "/home/dfreelan/dev/networks/singleLayerExample.prototxt";
        //Caffe.set_mode(Caffe.CPU);

        try {
            //String str = FileUtils.readFileToString(new File(paramFile), "utf-8");
            ///System.err.println(str);
            if (modelFile != null) {
                Thread.sleep(new Random().nextInt(10000+1000));
                caffe_net = new FloatNet(paramFile, TEST);
                caffe_net.CopyTrainedLayersFrom(modelFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        java.util.logging.Logger.getLogger(caffe.class.getSimpleName()).setLevel(Level.OFF);;
    }
    ExperimentalMCTS[] behaviours;

    @Override
    public synchronized GameCritique trainBasedOnActor(Behaviour a, GameContext startingTurn, Player p) {
        f = new FeatureCollector(startingTurn, p);
        if (caffe_net != null) {
            return this;
        }
        Random generator = new Random();

        f = new FeatureCollector(startingTurn, p);
        f.printFeatures(startingTurn, p);

        playerID = p.getId();
        startingTurn = startingTurn.clone();
        startingTurn.getPlayer1().setBehaviour(a.clone());
        startingTurn.getPlayer2().setBehaviour(a.clone());

        ArrayList<double[]> gameFeatures = new ArrayList<>();
        ArrayList<Double> gameLabels = new ArrayList<>();
        ArrayList<Double> gameWeights = new ArrayList<>();

        ArrayList<double[]> gameFeaturesTesting = new ArrayList<>();
        ArrayList<Double> gameLabelsTesting = new ArrayList<>();
        ArrayList<Double> gameWeightsTesting = new ArrayList<>();

        ArrayList<GameContext> testingSetContexts = new ArrayList<>();
        ArrayList<Double> testingSetReTests = new ArrayList<>();

        // 
        //System.err.println("on sample " + i);
        //GameContext simulation = startingTurn.clone();
        final GameContext simulation = startingTurn.clone();

        behaviours = new ExperimentalMCTS[samples * 2];
        IntStream.range(0, samples)
                .parallel()
                .forEach((int i) -> {
                    doPlay(RandomizeSimulation(simulation.clone()), i, f.clone());
                });
        System.err.println("game playing is done!");
        //System.exit(0);
        for (int i = 0; i < samples; i++) {
            boolean testing = !(i < samples - samples / 10);
            if (!testing) {
                gatherSamplesFrom(behaviours[i * 2], gameFeatures, gameLabels, gameWeights, null, 1);
                gatherSamplesFrom(behaviours[i * 2 + 1], gameFeatures, gameLabels, gameWeights, null, 0);
            } else {
                gatherSamplesFrom(behaviours[i * 2], gameFeaturesTesting, gameLabelsTesting, gameWeightsTesting, testingSetReTests, 1);
                gatherSamplesFrom(behaviours[i * 2 + 1], gameFeaturesTesting, gameLabelsTesting, gameWeightsTesting, testingSetReTests, 0);
            }
        }
        System.err.println("total weight: " + getSum(gameWeights));
        System.err.println("total ones: " + getNumOnes(gameWeights));
        double[][] trainingFeatures = new double[gameFeatures.size()][gameFeatures.get(0).length];
        double[][] trainingLabels = new double[gameLabels.size()][1];
        double[][] trainingWeights = new double[gameLabels.size()][1];
        for (int i = 0; i < trainingFeatures.length; i++) {
            trainingFeatures[i] = gameFeatures.get(i);
            double[] arr1 = new double[1];
            arr1[0] = gameLabels.get(i);
            trainingLabels[i] = arr1;
            arr1 = new double[1];
            arr1[0] = gameWeights.get(i);
            trainingWeights[i] = arr1;
        }
        sendCaffeData(trainingFeatures, trainingLabels, trainingWeights, "HearthstoneTrainingALLSTUFF.h5", false, false);//(int) (gameFeatures.size() * 1.6),true);

        // train.iteration(1000);
        double[][] testingFeatures = new double[gameFeaturesTesting.size()][gameFeaturesTesting.get(0).length];
        double[][] testingLabels = new double[gameLabelsTesting.size()][1];
        double[][] testingWeights = new double[gameLabelsTesting.size()][1];
        for (int i = 0; i < testingFeatures.length; i++) {
            testingFeatures[i] = gameFeaturesTesting.get(i);
            double[] arr1 = new double[1];
            arr1[0] = gameLabelsTesting.get(i);
            testingLabels[i] = arr1;
            arr1 = new double[1];
            arr1[0] = gameWeightsTesting.get(i);
            testingWeights[i] = arr1;

        }
        sendCaffeData(testingFeatures, testingLabels, testingWeights, "HearthstoneTestingALLSTUFF.h5", true, false);

        System.err.println("testing err if just 0's" + this.getErrorIfZero(testingLabels));
        System.err.println("ideal testing err: " + this.getIdealTestingError(testingLabels, testingSetContexts, testingSetReTests));

        return this;
    }

    public void doPlay(GameContext simulation, int index, FeatureCollector f) {

        System.err.println("on sample " + index);
        ExperimentalMCTS mctsP1 = new ExperimentalMCTS(rollouts, trees, exploreRate, false,false);
        mctsP1.doLog(f);
        ExperimentalMCTS mctsP2 = new ExperimentalMCTS(rollouts, trees, exploreRate, false,false);
        mctsP2.doLog(f);
        simulation = playSimulation(simulation, mctsP1, mctsP2);
        behaviours[index * 2] = mctsP1;
        behaviours[index * 2 + 1] = mctsP2;
    }

    public void gatherSamplesFrom(ExperimentalMCTS mctsP2, ArrayList<double[]> gameFeatures, ArrayList<Double> gameLabels, ArrayList<Double> gameWeights, ArrayList<Double> retestValues, int playerID) {

        for (int q = 0; q < mctsP2.samples.size(); q++) {
            MCTSSample sampleSeed = mctsP2.samples.get(q);
            MCTSSample allSamples[] = sampleSeed.expandSample();
            //System.err.println("there are " + mctsP2.samples.size() + " samples to choose from");
            for (MCTSSample sample : allSamples) {

                GameContext randomContext = sample.reachableState;
                double distribution = sample.winRate;

                //System.err.println("example of a label: " + distribution);
                if (Double.isNaN(distribution)) {
                    distribution = 0;
                } else {
                    distribution = distribution * 2 - 1;
                }

                ArrayList<Double> temp = new ArrayList<Double>();
                double[] info = sample.getFeatures();//f.getFeatures(true, randomContext, randomContext.getPlayer(playerID));
                //f.printFeatures(randomContexts[w], randomContexts[w].getPlayer2());
                double sum = 0;
                for (int d = 0; d < info.length; d++) {
                    sum += info[d] * (d + 1) * 2;
                }
                // System.err.println("feature vector is all like " + sum + " " + info.length);
                gameFeatures.add(info);
                gameLabels.add((Double) (double) distribution);
                gameWeights.add(sample.weight);
                if (retestValues != null) {
                    retestValues.add(sample.testValue);
                }
            }
        }
    }

    //probability 0-1 of winning from the player erspective
    @Override
    public synchronized double getCritique(GameContext context, Player p) {
        java.util.logging.Logger.getLogger(caffe.class.getSimpleName()).setLevel(Level.OFF);;
        context = context.clone();
        p = context.getPlayer(p.getId());
        Caffe.set_mode(Caffe.CPU);
        //System.err.println("network is " + network + " " + context + " " + f);
        double input[] = f.getFeatures(true, context, p);
        if (input.length != 169) {
            System.err.println("heyyyoo");
            System.exit(0);
        }
        //System.err.println("input length is " + input.length);
        int hash = 0;
        for (int i = 0; i < input.length; i++) {
            hash += (i + 1) * (input[i] + i);
        }
        //System.err.println("hash is " + hash);
        String paramFile = "/home/dfreelan/dev/networks/singleLayerExample.prototxt";
        // System.err.println("HPASE IS: " + caffe_net.phase());
        java.util.logging.Logger.getGlobal().setLevel(Level.OFF);
        //caffe_net.ClearParamDiffs();

        FloatBlob dataBlob = caffe_net.blob_by_name("data");

        //dataBlob.Reshape(169, 1, 1, 1);
        dataBlob.set_cpu_data(toFloats(input));

        dataBlob.Update();
        dataBlob.Update();

        // System.err.println("bott blob length? "  + bottom.size());
        //FloatBlobVector top = caffe_net.ForwardPrefilled();
        caffe_net.ForwardFrom(1);
        float out = caffe_net.blob_by_name("tanhFinal").cpu_data().get();

        dataBlob.set_cpu_data(new float[169]);

        dataBlob.Update();
        dataBlob.Update();

        // System.err.println("bott blob length? "  + bottom.size());
        //FloatBlobVector top = caffe_net.ForwardPrefilled();
        caffe_net.ForwardFrom(1);
        float out2 = caffe_net.blob_by_name("tanhFinal").cpu_data().get();
        if (Float.isNaN(out) || out2 == out) {
            System.err.println("is nan you  or producing same output..." + out + " " + out2);
            out = 0;
            //f.printFeatures(context, p);
            // modelFile = "/home/dfreelan/dev/networks/_iter_350000.caffemodel";
            paramFile = "/home/dfreelan/dev/networks/singleLayerExample.prototxt";
            Caffe.set_mode(Caffe.CPU);

            try {
                //String str = FileUtils.readFileToString(new File(paramFile), "utf-8");
                ///System.err.println(str);
                if (modelFile != null) {
                    caffe_net = new FloatNet(paramFile, TEST);
                    caffe_net.CopyTrainedLayersFrom(modelFile);
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            return getCritique(context, p);
        }

        //System.err.println(" i made a deicsion! "  + out);
        return (out + 1.0) / 2.0;
        //NeuralDataSet sample = new BasicNeuralDataSet(new double[][] 
        //{ f.getFeatures(true, context, context.getPlayer(playerID)) }); 

    }

    public double getSum(ArrayList<Double> weights) {
        double sum = 0.0;

        for (Double a : weights) {
            sum += a;
        }

        return sum;
    }

    public double getNumOnes(ArrayList<Double> weights) {
        double sum = 0.0;

        for (Double a : weights) {
            if (a == 1.0) {
                sum += a;
            }
        }

        return sum;
    }

    public float[] toFloats(double[] doubles) {
        float[] floats = new float[doubles.length];
        int i = 0;
        for (double a : doubles) {
            floats[i] = (float) a;
            i++;
        }
        return floats;
    }

    @Override
    public GameCritique clone() {

        MCTSCritique clone = new MCTSCritique(modelFile);
        clone.f = this.f.clone();

        return clone;
    }

    public GameContext playSimulation(GameContext simulation, ExperimentalMCTS mctsP1, ExperimentalMCTS mctsP2) {
        this.RandomizeSimulation(simulation);

        simulation.getPlayer1().setBehaviour(mctsP1);
        simulation.getPlayer2().setBehaviour(mctsP2);

        simulation.play();
        return simulation;
    }

    public GameContext RandomizeSimulation(GameContext simulation) {
        simulation.getPlayer2().getDeck().shuffle();
        simulation.getPlayer1().getDeck().shuffle();
        Player p1 = simulation.getPlayer1();;
        Player p2 = simulation.getPlayer2();

        p1.getDeck().addAll(p1.getHand());
        int handSize = p1.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = p1.getHand().get(0);
            simulation.getLogic().removeCard(p1.getId(), card);
        }
        p1.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            simulation.getLogic().receiveCard(p1.getId(), p1.getDeck().removeFirst());
        }

        p2.getDeck().addAll(p2.getHand());
        handSize = p2.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = p2.getHand().get(0);
            simulation.getLogic().removeCard(p2.getId(), card);
        }
        p2.getDeck().shuffle();
        for (int a = 0; a < handSize; a++) {
            simulation.getLogic().receiveCard(p2.getId(), p2.getDeck().removeFirst());
        }

        return simulation;
    }

    public void sendCaffeData(double[][] data2, double[][] labels2, double[][] weights, String name, boolean ordered, int numItemsInBatch, int numBatches, boolean file) {
        double[][] batchedData = new double[numBatches * numItemsInBatch][];
        double[][] batchedLabels = new double[numBatches * numItemsInBatch][];
        double[][] batchedWeights = new double[numBatches * numItemsInBatch][];
        Random generator = new Random();

        for (int i = 0; i < batchedData.length; i++) {
            if (i % 1000 == 0) {
                System.err.println("i is " + i);
            }
            int randomIndex = generator.nextInt(data2.length);
            batchedData[i] = data2[randomIndex];
            batchedLabels[i] = labels2[randomIndex];
            batchedWeights[i] = weights[randomIndex];
        }

        sendCaffeData(batchedData, batchedLabels, batchedWeights, name, true, file);

    }

    public void sendCaffeData(double[][] data2, double[][] labels2, double[][] weights, String name, boolean ordered, boolean file) {
        double[][] data = new double[data2.length][];
        double[][] labels = new double[data2.length][];
        if (!ordered) {
            for (int i = 0; i < data2.length; i++) {
                data[i] = data2[i].clone();
                labels[i] = labels2[i].clone();
            }
        } else {
            data = data2;
            labels = labels2;
        }
        try {
            String sentence;
            String modifiedSentence;
            Random generator = new Random();
            //set up our socket server

            BufferedReader inFromServer = null;
            DataOutputStream outToServer = null;
            Socket clientSocket = null;

            //add in data in a random order
            if (!file) {
                System.err.println("starting to send on socket");
                clientSocket = new Socket("localhost", 5004);
                clientSocket.setTcpNoDelay(false);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer.writeBytes(name + "\n");

                outToServer.writeBytes((data.length + " " + data[0].length + " " + labels[0].length) + "\n");
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                }
            } else {
                outToServer = new DataOutputStream((OutputStream) new FileOutputStream(name));
            }
            StringBuffer wholeMessage = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                if (i % 1000 == 0) {
                    System.err.println("(constructed) i is " + i);
                }
                String features = "";
                int randomIndex = generator.nextInt(data.length - i);

                if (!ordered) {
                    swap(data, i, i + randomIndex);
                    swap(labels, i, i + randomIndex);
                }
                for (int a = 0; a < data[i].length; a++) {
                    wholeMessage.append(data[i][a] + " ");
                }
                wholeMessage.append("\n");
                String myLabels = "";
                for (int a = 0; a < labels[i].length; a++) {
                    wholeMessage.append(labels[i][a] + " ");
                }
                wholeMessage.append("\n");
                wholeMessage.append(weights[i][0] + "");
                wholeMessage.append("\n");
                outToServer.writeBytes(wholeMessage.toString());
                wholeMessage = new StringBuffer();

            }
            System.err.println("total message size is " + wholeMessage.toString().length());

            //outToServer.writeBytes(wholeMessage.toString());
            if (!file) {
                System.err.println("sending done");
                outToServer.writeBytes("done\n");
                System.err.println("waiting for ack...");
                inFromServer.readLine();
                System.err.println("got the ack!");
                clientSocket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("server wasn't waiting");
        }
        System.err.println("hey i sent somethin!");

    }

    public void swap(double[][] data, int index1, int index2) {
        double[] temp = data[index1];
        data[index1] = data[index2];
        data[index2] = temp;
    }

    private double getErrorIfZero(double[][] testingLabels) {
        double sum = 0;
        for (int i = 0; i < testingLabels.length; i++) {
            sum += testingLabels[i][0] * testingLabels[i][0];
        }
        return sum / ((double) testingLabels.length);
    }

//just do hdf5 in java
    private double getIdealTestingError(double[][] labels, ArrayList<GameContext> games, ArrayList<Double> testValues) {
        double sum = 0;

        for (int i = 0; i < labels.length; i++) {
            if (testValues.size() != labels.length) {
                System.err.println("MISMATCH!");
                System.exit(0);
            }

            double actualResult = testValues.get(i);

            sum += ((labels[i][0] - actualResult) * (labels[i][0] - actualResult)) / ((double) labels.length);
            if (Double.isInfinite((labels[i][0] - actualResult) * (labels[i][0] - actualResult))) {
                System.err.println("HEY FUCK UP!! " + labels[i][0] + " " + actualResult);
            }
            System.err.println("test value was " + labels[i][0] + " actual value " + actualResult + " " + labels.length + " " + testValues.size());

        }
        return sum;

    }

}

class FileOutStream extends OutputStream {
    //DataOutputStream(OutputStream out)

    File file = null;

    BufferedWriter output = null;

    public FileOutStream(String fileName) {
        try {
            file = new File(fileName);
            output = new BufferedWriter(new FileWriter(file));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void write(byte[] b) {
        try {
            output.write(new String(b));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
