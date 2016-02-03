/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.demilich.metastone.game.behaviour.decicionTreeBheavior;

import gnu.trove.map.hash.TIntIntHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.minions.Minion;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;
import sim.app.horde.classifiers.Domain;
import sim.app.horde.classifiers.Example;
import sim.app.horde.classifiers.decisiontree.DecisionTree;

/**
 *
 * @author dfreelan
 */
final class DecisionDataBase implements Cloneable {

    Random random = new Random();
    //contains an array that holds the features
    //can query furfillOrder to populate the data
    //need to be initialized with # minions and spells in each deck
    TIntIntHashMap hashToIndex;
    TIntIntHashMap featureHashToIndex;

    int enemyMinionStart;
    int myHandStart;
    int enemyHandStart;
    int remainingInfoStart;
    static int additionalFeatures = 10;
    int length = 0;
    CardContextKnowledge[] database;//array of card knowledge
    String[] featureNames;//we need to know the name of all the actions for the decision tree
    int[][] featureAndHash;// sort of redundant with FeatureDetail, but this is more useful for querying the entire board
    //^^  USED for querying the entire board state
    int databaseCount = 0;
    int myMinionIndex = 0;
    int enemyMinionIndex = 0;
    int myHandIndex = 0;
    int enemyHandIndex = 0;
    int remainingInfoIndex = 0;

    double[] featureData; //feature array, will be resued; 
    boolean modified = false;

    public synchronized DecisionDataBase clone() {
        DecisionDataBase clone = new DecisionDataBase();
        /* int enemyMinionStart;
         int myHandStart;
         int enemyHandStart;
         int remainingInfoStart;*/
        clone.enemyHandStart = this.enemyHandStart;
        clone.enemyMinionStart = this.enemyMinionStart;
        clone.remainingInfoStart = this.remainingInfoStart;
        clone.myHandStart = this.myHandStart;

        clone.database = this.database;
        clone.featureNames = this.featureNames;
        clone.featureAndHash = this.featureAndHash;
        clone.featureData = this.featureData.clone();
        clone.hashToIndex = this.hashToIndex;
        clone.featureHashToIndex = this.featureHashToIndex;
        clone.modified = true;
        return clone;
    }

    private DecisionDataBase() {
    }

    public DecisionDataBase(GameContext context, Player player) {

        int myMinionCount = 0;
        int enemyMinionCount = 0;
        int mySpellCount = 0;
        int enemySpellCount = 0;
        TIntIntHashMap enemyMap = new TIntIntHashMap(60);
        TIntIntHashMap myMap = new TIntIntHashMap(60);
        featureHashToIndex = new TIntIntHashMap(240);
        for (Card card : player.getHand()) {
            int hash = card.getName().hashCode();
            if (myMap.contains(hash)) {
                continue;
            }
            myMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                myMinionCount++;
            } else {
                mySpellCount++;
            }
        }
        for (Card card : context.getOpponent(player).getHand()) {
            int hash = card.getName().hashCode();
            if (enemyMap.contains(hash)) {
                continue;
            }
            enemyMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                enemyMinionCount++;
            } else {
                enemySpellCount++;
            }
        }

        for (Card card : player.getDeck()) {
            int hash = card.getName().hashCode();
            if (myMap.contains(hash)) {
                continue;
            }
            myMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                myMinionCount++;
            } else {
                mySpellCount++;
            }
        }

        for (Card card : context.getOpponent(player).getDeck()) {
            int hash = card.getName().hashCode();
            if (enemyMap.contains(hash)) {
                continue;
            }
            enemyMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                enemyMinionCount++;
            } else {
                enemySpellCount++;
            }
        }
        init(myMinionCount, enemyMinionCount, mySpellCount, enemySpellCount);
        enemyMap.clear();
        myMap.clear();
        for (Card card : player.getHand()) {
            int hash = card.getName().hashCode();
            if (myMap.contains(hash)) {
                continue;
            }
            myMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                addFeature(card.getName(), false, true);
            } else {
                addFeature(card.getName(), false, false);
            }
        }
        for (Card card : context.getOpponent(player).getHand()) {
            int hash = card.getName().hashCode();
            if (enemyMap.contains(hash)) {
                continue;
            }
            enemyMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                addFeature(card.getName(), true, true);
            } else {
                addFeature(card.getName(), true, false);
            }
        }

        for (Card card : player.getDeck()) {
            int hash = card.getName().hashCode();
            if (myMap.contains(hash)) {
                continue;
            }
            myMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                addFeature(card.getName(), false, true);
            } else {
                addFeature(card.getName(), false, false);
            }
        }

        for (Card card : context.getOpponent(player).getDeck()) {
            int hash = card.getName().hashCode();
            if (enemyMap.contains(hash)) {
                continue;
            }
            enemyMap.put(hash, 0);
            if (card.getCardType() == CardType.MINION) {
                addFeature(card.getName(), true, true);
            } else {
                addFeature(card.getName(), true, false);
            }
        }

        addAdditionalFeatures();
        initClassifiers();
        modified = true;
        reset();
        for (int i = 0; i < featureAndHash[0].length; i++) {
            System.err.println("table: " + featureAndHash[0][i] + " " + featureAndHash[1][i] + " " + featureNames[i]);
        }
    }

    public void reUseData() {
        modified = false;
    }

    public void learn() {
        for (CardContextKnowledge k : database) {
            k.learn(featureAndHash[1]);
        }
    }

    private void initClassifiers() {
        DecisionTree defaultTree;
        Domain defaultDomain;
        for (CardContextKnowledge k : database) {
            defaultDomain = getDefaultDomain();
            defaultDomain.name = k.cardName;
            defaultTree = new DecisionTree(defaultDomain);
            k.init(defaultTree, featureNames);
        }
    }

    private Domain getDefaultDomain() {
        String[] attributeNames = this.featureNames.clone();
        String classNames[] = {"BAD", "meh", "GOOD"};
        String[][] attributeValueNames = new String[length - additionalFeatures][2];
        for (int i = 0; i < attributeValueNames.length; i++) {
            attributeValueNames[i][0] = "no";
            attributeValueNames[i][0] = "yes";
        }
        double[][] valueRange = {};
        int[] attributeType = new int[length];
        for (int i = 0; i < length - additionalFeatures; i++) {
            attributeType[i] = 0;//categorical, yes or no
        }
        for (int i = length - additionalFeatures; i < length; i++) {
            attributeType[i] = 1; //continuous value (ints are "continuous" in this case)
        }
        return new Domain("Card Tree", attributeNames, attributeValueNames, classNames, valueRange, attributeType);

    }

    public DecisionDataBase(int myMinionCount, int enemyMinionCount, int mySpellCount, int enemySpellCount) {
        init(myMinionCount, enemyMinionCount, mySpellCount, enemySpellCount);
    }

    public void init(int myMinionCount, int enemyMinionCount, int mySpellCount, int enemySpellCount) {
        database = new CardContextKnowledge[myMinionCount + mySpellCount];

        hashToIndex = new TIntIntHashMap(60, 1.0f, -1, -1);//default -1 if there's no entry, also there can technically be 60 cardsin a deck
        //public TIntIntHashMap( int initialCapacity, float loadFactor,
        //int noEntryKey, int noEntryValue )
        enemyMinionStart = myMinionCount;
        myHandStart = enemyMinionStart + enemyMinionCount;
        enemyHandStart = myHandStart + (myMinionCount + mySpellCount);
        remainingInfoStart = enemyHandStart + enemyMinionCount + enemySpellCount;

        myMinionIndex = 0;
        enemyMinionIndex = enemyMinionStart;
        myHandIndex = myHandStart;
        enemyHandIndex = enemyHandStart;
        remainingInfoIndex = remainingInfoStart;
        length = remainingInfoStart + additionalFeatures;

        featureAndHash = new int[2][length];
        featureNames = new String[length];
        featureData = new double[length];
    }

    public void addFeature(String name, boolean enemy, boolean minion) {
        int hash = name.hashCode();
        if (!enemy) {
            if (!hashToIndex.containsKey(hash)) {
                database[databaseCount] = new CardContextKnowledge(name, hash);
                hashToIndex.put(hash, databaseCount);
                databaseCount++;
            }
        }
        if (enemy) {
            featureAndHash[0][enemyHandIndex] = enemyHandIndex;
            featureAndHash[1][enemyHandIndex] = hash;
            featureNames[enemyHandIndex] = "EH:" + name;
            this.featureHashToIndex.put(featureNames[enemyHandIndex].hashCode(), enemyHandIndex);
            enemyHandIndex++;
            if (minion) {
                featureAndHash[0][enemyMinionIndex] = enemyMinionIndex;
                featureAndHash[1][enemyMinionIndex] = hash;
                featureNames[enemyMinionIndex] = "EB:" + name;
                this.featureHashToIndex.put(featureNames[enemyMinionIndex].hashCode(), enemyMinionIndex);
                enemyMinionIndex++;
            }
        } else {
            featureAndHash[0][myHandIndex] = myHandIndex;
            featureAndHash[1][myHandIndex] = hash;
            featureNames[myHandIndex] = "MH:" + name;
            this.featureHashToIndex.put(featureNames[myHandIndex].hashCode(), myHandIndex);
            myHandIndex++;
            if (minion) {
                featureAndHash[0][myMinionIndex] = myMinionIndex;
                featureAndHash[1][myMinionIndex] = hash;
                featureNames[myMinionIndex] = "MB:" + name;
                this.featureHashToIndex.put(featureNames[myMinionIndex].hashCode(), myMinionIndex);
                myMinionIndex++;
            }
        }
    }

    public int getFeatureIndex(String name, boolean enemy, boolean board) {

        String featureName;
        if (enemy) {
            if (board) {
                featureName = "EB:";
            } else {
                featureName = "EH:";
            }
        } else {
            if (board) {
                featureName = "MB:";
            } else {
                featureName = "MH:";
            }
        }
        featureName += name;

        return this.featureHashToIndex.get(featureName.hashCode());
    }

    public synchronized void reset() {
        if (modified) {
            for (int i = 0; i < featureData.length; i++) {
                featureData[i] = -1;
            }
        }
        modified = false;
    }
    boolean deciding = false;
    int prevTurn = -1;
    //need a method that adds an example for the particular card
    //need a method that gets a decision

    public double getDecision(GameContext context, String actionName, Player player) {
        int hash = actionName.hashCode();
        int index = hashToIndex.get(hash);
        if (index == -1) {
            //System.err.println("couldn't find card " + actionName);
            return 0;
        }
        CardContextKnowledge knowledge = database[index];
        if (prevTurn == context.getTurn()) {
            modified = false;
        } else {
            prevTurn = context.getTurn();

        }
        furfillOrder(context, knowledge, player);
        //Example query = new Example(featureData);

        return knowledge.classify(featureData);
        // return knowledge.probMax.percentGood;
    }

    public void printEntireBoardState(GameContext context, Player player) {
        reset();

        furfillOrder(context, featureAndHash[0], featureAndHash[1], player);

        this.printDataState();
    }

    public synchronized void addExample(GameContext context, String actionName, Player player, int label) {

        //we want to add an example to the knowledge that hash the same actionName
        int index = hashToIndex.get(actionName.hashCode());
        if (index == -1) {
            System.err.println("could not find " + actionName);
            return;
        }
        CardContextKnowledge knowledge = database[index];

        //populate feature data
        reset();
        this.furfillOrder(context, this.featureAndHash[0], null, player);
        //furfillAll(context,player);

        //add the example
        knowledge.addExample(this.featureData.clone(), label);

    }

    private void furfillAll(GameContext context, Player player) {
        //eatureAndHash[1][features[i]], (ArrayList<Minion>) player.getMinions()
        reset();
        modified = true;
        populateBoardMinions((ArrayList<Minion>) player.getMinions(), false);
        populateBoardMinions((ArrayList<Minion>) context.getOpponent(player).getMinions(), true);
        populateHandCards((ArrayList<Card>) player.getHand().getList(), false);
        populateHandCards((ArrayList<Card>) context.getOpponent(player).getHand().getList(), true);
    }

    private void populateBoardMinions(ArrayList<Minion> minions, boolean enemy) {
        for (Minion minion : minions) {
            int index = this.getFeatureIndex(minion.getName(), enemy, true);
            this.featureData[index] = 1;
        }
    }

    private void populateHandCards(ArrayList<Card> cards, boolean enemy) {
        for (Card card : cards) {
            int index = this.getFeatureIndex(card.getName(), enemy, false);
            this.featureData[index] = 1;
        }
    }

    private void furfillOrder(GameContext context, CardContextKnowledge knowledge, Player player) {
        if (knowledge.requiredFeatures == null) {
            System.err.println("REQUIRED FEATURES WAS NULL");
            throw new RuntimeException("REQUIRED FEATURE WAS NULL");

        }
       // if (knowledge.requiredFeatures.length > 5) {
        //   furfillAll(context, player);
        //}else{
        furfillOrder(context, knowledge.requiredFeatures, knowledge.featureHashes, player);
        //}

    }
    /*
     if (!context.getSummonStack().isEmpty()) {
     Minion summonedMinion = context.getSummonStack().peek();
     if (summonedMinion.getId() == targetId) {
     return summonedMinion;
     }
     }
     */

    private void furfillOrder(GameContext context, int[] features, int[] featureHashes, Player player) {
        reset();
        modified = true;
        for (int i = 0; i < features.length; i++) {
            double value = -1;
            //System.err.println("feature " + features[i] + " -> " + featureAndHash[1][features[i]] + " " + i);
            if (features[i] < enemyMinionStart) {//we're in friendly minion territory
                value = findMinionWithHash(featureAndHash[1][features[i]], (ArrayList<Minion>) player.getMinions());
            } else if (features[i] < myHandStart) {//we're in ENEMY MINION territory
                value = findMinionWithHash(featureAndHash[1][features[i]], (ArrayList<Minion>) context.getOpponent(player).getMinions());
            } else if (features[i] < enemyHandStart) { //My HAND start
                value = findCardWithHash(featureAndHash[1][features[i]], (ArrayList<Card>) player.getHand().getList());
            } else if (features[i] < remainingInfoStart) { // enemyHAND start
                value = findCardWithHash(featureAndHash[1][features[i]], (ArrayList<Card>) context.getOpponent(player).getHand().getList());
            } else {
                int featureSelect = features[i] - remainingInfoStart;
                switch (featureSelect) {
                    case 0:
                        value = player.getMinions().size() / 4.0;
                        break;
                    case 1:
                        value = context.getOpponent(player).getMinions().size() / 4.0;
                        break;
                    case 2:
                        value = player.getHero().getEffectiveHp() / 30.0;
                        break;
                    case 3:
                        value = context.getOpponent(player).getHero().getEffectiveHp() / 30.0;
                        break;
                    case 4:
                        value = player.getDeck().getCount() / 30.0;
                        break;
                    case 5:
                        value = context.getOpponent(player).getDeck().getCount() / 30.0;
                        break;
                    case 6:
                        value = player.getHand().getCount() / 10.0;
                        break;
                    case 7:
                        value = context.getOpponent(player).getHand().getCount() / 10.0;
                        break;
                    case 8:
                        value = player.getMana() / 10.0;
                        break;
                    case 9:
                        value = context.getOpponent(player).getMaxMana() / 10.0;
                        break;
                    default:
                        throw new RuntimeException("you tried to get a feature which shouldnt exist #" + features[i] + " size:" + featureData.length);

                }
            }
            featureData[features[i]] = value;
        }
    }

    private void addAdditionalFeatures() {
        for (int i = 0; i < additionalFeatures; i++) {
            featureAndHash[0][remainingInfoIndex + i] = remainingInfoIndex + i;
            featureAndHash[1][remainingInfoIndex + i] = -1;
        }
        featureNames[remainingInfoIndex] = "#MyMinion";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#EnMinion";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#MyHp";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#EnHp";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#MDeckCount";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#EDeckCount";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#MHandCount";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#EHandCount";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#MyMana";
        remainingInfoIndex++;
        featureNames[remainingInfoIndex] = "#EnMana";
    }

    public int findCardWithHash(int hash, ArrayList<Card> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().hashCode() == hash) {
                return 1;
            }
        }
        return 0;
    }

    public int findMinionWithHash(int hash, ArrayList<Minion> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().hashCode() == hash) {
                return 1;
            }
        }
        return 0;
    }

    public void printDataState() {
        System.err.println("my board:");

        for (int i = 0; i < this.enemyMinionStart; i++) {
            if (featureData[i] == 1) {
                System.err.println(this.featureNames[i] + " -> " + i);
            }
        }
        System.err.println("enemy board:");
        for (int i = enemyMinionStart; i < this.myHandStart; i++) {
            if (featureData[i] == 1) {
                System.err.println(this.featureNames[i]);
            }
        }
        System.err.println("my Hand:");
        for (int i = myHandStart; i < this.enemyHandStart; i++) {
            if (featureData[i] == 1) {
                System.err.println(this.featureNames[i]);
            }
        }
        System.err.println("enemy Hand:");
        for (int i = this.enemyHandStart; i < this.remainingInfoStart; i++) {
            if (featureData[i] == 1) {
                System.err.println(this.featureNames[i]);
            }
        }
    }

    public CardContextKnowledge getCardKnowledge(String name) {
        int index = this.hashToIndex.get(name.hashCode());
        if (index == -1) {
            System.err.println("could not find " + name);
            return null;
        }
        return database[this.hashToIndex.get(name.hashCode())];
    }
}

class FeatureStats implements Comparable<FeatureStats> {

    final int feature;
    int value;
    double pValue;
    double percentGood;
    double frequency;
    String featureName;
    double defaultScore;

    public FeatureStats(int feature, double pValue, double percentGood, int value, double frequency, String featureName, double defaultScore) {
        this.feature = feature;
        this.defaultScore = defaultScore;
        //this.pValue = pValue;
        if (Double.isNaN(pValue)) {
            this.pValue = 0;
        } else {
            this.pValue = Math.max(1 - pValue, pValue);
        }
        this.percentGood = percentGood;
        if (Double.isNaN(percentGood)) {
            this.percentGood = 0;
        }
        this.value = value;
        this.frequency = frequency;
        this.featureName = featureName;
    }

    @Override
    public int compareTo(FeatureStats o) {

        if (pValue > o.pValue) {
            return -1;
        } else if (pValue < o.pValue) {
            return 1;
        }
        return 0;
    }

}

class MaxBiasedProbability {

    int[] usedFeatures;
    static int numFeaturesUsed = 5;
    static double threshold = .85;
    ArrayList<FeatureStats> stats = new ArrayList<>();
    String[] featureNames;
    double percentGood;

    public MaxBiasedProbability(String[] featureNames) {
        usedFeatures = new int[numFeaturesUsed];
        this.featureNames = featureNames;
    }

    public void printInfo() {
        for (int i = 0; i < usedFeatures.length; i++) {
            System.err.println(stats.get(i).featureName + "=" + stats.get(i).value + "> good%:" + stats.get(i).percentGood + ", Pvalue:" + stats.get(i).pValue + " id" + stats.get(i).feature);
        }
        System.err.println("general strength of card: " + percentGood);
    }

    public synchronized void learn(Example[] examples) {
        double totalGood = Arrays.asList(examples).stream().filter(e -> e.classification == 2).count();
        NormalDistribution norm = new NormalDistribution();
        this.percentGood = totalGood / examples.length;
        for (int i = 0; i < examples[0].values.length; i++) {
            final int feature = i; //i feel like smeting should be wrong with this.... doesn't compile without the final variable
            //System.err.println("feature: " + feature);

            List<Example> featureTrue = Arrays.asList(examples).stream()
                    .filter(e -> e.values[feature] > .5)
                    .collect(Collectors.toList());

            makeNewStat(featureTrue, feature, totalGood, norm, (double) examples.length, 1);

            List<Example> featureFalse = Arrays.asList(examples).stream()
                    .filter(e -> e.values[feature] < .5)
                    .collect(Collectors.toList());

            makeNewStat(featureFalse, feature, totalGood, norm, (double) examples.length, 0);
        }

        Collections.sort(stats);
        for (int i = 0; i < usedFeatures.length; i++) {
            usedFeatures[i] = stats.get(i).feature;
            if (stats.get(i).pValue < MaxBiasedProbability.threshold) {
                stats.get(i).percentGood = this.percentGood;
            }
        }
    }

    private synchronized void makeNewStat(List<Example> featureTrue, int feature, double totalGood, NormalDistribution norm, double numSamples, int value) {
        double numGoodTrue = (double) featureTrue.stream().filter(e -> e.classification == 2).count();

        double pValue = getZScore(numSamples, totalGood, featureTrue.size(), numGoodTrue);
        pValue = norm.cumulativeProbability(pValue);
        //if(pValue<.5){
        //    pValue = 1.0-pValue;
        // }
        //if(numGoodTrue/featureTrue.size() > .5){
        //   pValue = pValue+.02;
        // }
        //System.err.println("stats is " + numGoodTrue+ " size is " + featureTrue.size());
        //public FeatureStats(int feature, double pValue, double percentGood, int value, String featureName){
        stats.add(new FeatureStats(feature, pValue, numGoodTrue / featureTrue.size(), value, ((double) featureTrue.size()) / numSamples, featureNames[feature], this.percentGood));
    }

    public synchronized double getZScore(double dist1Samples, double dist1Positives, double dist2Samples, double dist2Positives) {
        double p1 = dist1Positives / dist1Samples;
        double p2 = dist2Positives / dist2Samples;
        double pHat = (dist1Samples * p1 + dist2Samples * p2) / (dist1Samples + dist2Samples);
        return (p1 - p2) / Math.sqrt(pHat * (1 - pHat) * (1 / dist1Samples + 1 / dist2Samples));
        //BinomialTest dist = new BinomialTest();
        //System.err.println(dist2Samples + " " + dist2Positives + " is returning:" + dist.binomialTest((int) dist2Samples, (int) dist2Positives, .5, AlternativeHypothesis.TWO_SIDED));
        //return 1 - dist.binomialTest((int) dist2Samples, (int) dist2Positives, .5, AlternativeHypothesis.TWO_SIDED);
    }
//for each feature f
    //for each value a feature can have i = (>.5, <.5)
    //find all the good and bad examples that have that value of that feature
    //store good/(good+bad) as p(good|f=i)
    //end for
    //end for
    // we want to find the top numFeaturesUsed indicators
    //of whether or not a card is good to play or not
    //there is some tradeoff of knowing how many 
    //samples
    //if it only happened once, the utility of its
    //estimate is very small.
    //so what is the utility of the estimate?
    //given, p(f=i), p(good|f=i), how many more games will I win if I keep this statistic?
    //p(f=i)*max(p(good|f=i),1-p(good|f=i)) = % more games i'll win if i keep this statistic

    public void sumArrays(double[] src, double[] dest) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] += src[i];
        }
    }

    public double getDecision(double[] data) {
        double worstAttribute = this.percentGood;

        for (int i = 0; i < this.usedFeatures.length; i++) {
            FeatureStats stats = this.stats.get(i);
            //System.err.println("checking feature " + stats.featureName + " " + "#" + stats.feature);
            //System.err.println("data[feature]=" +data[stats.feature] + " desired value:" + stats.value);

            if (data[stats.feature] < .5) {
                data[stats.feature] = 0;
            } else {
                data[stats.feature] = 1;
            }
            if (data[stats.feature] == stats.value) {
                //  System.err.println("we matched the feature " + stats.featureName + "=" + stats.value);
                //  System.err.println("it is " + this.percentGood + " on its own");
                //  System.err.println("but in this situation: " + stats.percentGood);
                if (stats.percentGood < worstAttribute) {
                    worstAttribute = stats.percentGood;
                }
            }
        }
        //System.err.println("we didn't find a matching feature");

        return worstAttribute;
    }
}

class CardContextKnowledge {

    static int learningType = 1;
    DecisionTree tree;
    MaxBiasedProbability probMax;
    String cardName;
    int hash;
    int[] requiredFeatures;
    int[] featureHashes;
    int numGoodLabel;
    int numBadLabel;
    int numMehLabel;
    boolean needsMehLabels = false;
    Random random = new Random();
    String[] featureNames;
    ArrayList<Example> examples = new ArrayList<Example>();

    public CardContextKnowledge(String cardName, int hash) {
        this.hash = hash;
        this.cardName = cardName;
    }

    public void init(DecisionTree tree, String[] featureNames) {
        this.featureNames = featureNames;
        this.tree = tree;
    }

    public double classify(double[] data) {
        //  System.err.println("classying for card: " + this.cardName);
        switch (learningType) {
            case 0:
                return doTreeClassify(data);
            case 1:
                return doMaxProbClassify(data);
        }
        throw new RuntimeException("invalid learning method " + learningType);
    }

    public double doMaxProbClassify(double[] data) {
        return probMax.getDecision(data);
    }

    public void learnMaxProb(int[] featureHashes) {
        probMax = new MaxBiasedProbability(featureNames);
        probMax.learn(examples.toArray(new Example[examples.size()]));

        this.requiredFeatures = probMax.usedFeatures;
        initFeatureHashes(featureHashes);
    }

    public int doTreeClassify(double[] data) {
        if (examples.size() > 0) {
            Example exam = new Example(data);
            return tree.classify(exam);
        }
        return random.nextInt(3);
    }

    private void initFeatureHashes(int[] featureHashes) {
        if (this.cardName.equals("Brawl")) {
            System.err.println("brawl requires the following features: ");
        }
        this.featureHashes = new int[requiredFeatures.length];
        for (int i = 0; i < requiredFeatures.length; i++) {
            if (this.cardName.equals("Brawl")) {
                System.err.println(featureNames[requiredFeatures[i]] + "->" + featureNames[requiredFeatures[i]].substring(3).hashCode());
                System.err.println("hash i'm sending: " + featureHashes[requiredFeatures[i]]);
                System.err.println("feature number: " + requiredFeatures[i]);
            }

            this.featureHashes[i] = featureHashes[requiredFeatures[i]];
        }
    }

    public synchronized void learn(int[] featureHashes) {
        if (examples.size() > 0) {
            switch (learningType) {
                case 0:
                    learnDecisionTree(featureHashes);
                    break;
                case 1:
                    learnMaxProb(featureHashes);
                    break;
            }
        }
    }

    public void learnDecisionTree(int[] featureHashes) {
        tree.learn(examples.toArray(new Example[examples.size()]), 2);
        requiredFeatures = tree.getRoot().getUsedAttributes();
        initFeatureHashes(featureHashes);
        if (requiredFeatures != null) {
            System.err.println("features required is : " + requiredFeatures.length);
        } else {
            System.err.println("no features required");
        }
    }

    public synchronized void addExample(double[] features, int label) {
        switch (label) {
            case 0:
                numBadLabel++;
                break;
            case 1:
                numGoodLabel++;
                break;
            case 2:
                numMehLabel++;
                break;
        }
        if ((numGoodLabel + numBadLabel) / 2 > numMehLabel) {
            needsMehLabels = true;
        } else {
            needsMehLabels = false;
        }
        Example newExample = new Example(features, label);
        examples.add(newExample);
    }
}
