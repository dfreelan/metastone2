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
public final class FeatureCollector implements Cloneable {

    Random random = new Random();
    //contains an array that holds the features
    //can query furfillOrder to populate the data
    //need to be initialized with # minions and spells in each deck
    TIntIntHashMap enemyCardMap = new TIntIntHashMap(60);
    TIntIntHashMap myCardMap = new TIntIntHashMap(60);

    TIntIntHashMap reverseMyCardMap = new TIntIntHashMap(60);
    TIntIntHashMap reverseEnemyCardMap = new TIntIntHashMap(60);
    double[] featureData; //feature array, will be resued; 
    boolean modified = false;
    ArrayList<Integer> cardCount = new ArrayList<Integer>();
    ArrayList<String> cardNames = new ArrayList<String>();
    
    ArrayList<Integer> enemyCardCount = new ArrayList<Integer>();
    ArrayList<String> enemyCardNames = new ArrayList<String>();
    
    int lastSelfCard ;
    int lastEnemyCard ;
    int featureCount = 0;
    int myBoardStart;
    int enemyBoardStart;
    public synchronized FeatureCollector clone() {
        FeatureCollector clone = new FeatureCollector();
        return clone;
    }
     public FeatureCollector(GameContext context, Player player) {
        

        //self is hand,deck,played
        for (Card card : player.getHand()) {
            int hash = card.getName().hashCode();
            if(!myCardMap.containsKey(hash)){
                myCardMap.put(hash, featureCount);
                reverseMyCardMap.put(featureCount,hash);
                this.cardNames.add(card.getName());
                cardCount.add(1);
                featureCount+=3;
            }else{
                cardCount.set(myCardMap.get(hash)/3,2);
            }
        }
        for (Card card : player.getDeck()){
             int hash = card.getName().hashCode();
            if(!myCardMap.containsKey(hash)){
                myCardMap.put(hash, featureCount);
                reverseMyCardMap.put(featureCount,hash);
                this.cardNames.add(card.getName());
                cardCount.add(1);
                featureCount+=3;
            }else{
                cardCount.set(myCardMap.get(hash)/3,2);
            }
        }
        
        lastSelfCard = featureCount-3;
        
        Player opponent = context.getOpponent(player);
        //data is deck,played
        for (Card card : opponent.getHand()) {
             int hash = card.getName().hashCode();
            if(!enemyCardMap.containsKey(hash)){
                enemyCardMap.put(hash, featureCount);
                
                reverseEnemyCardMap.put(featureCount,hash);
                this.enemyCardNames.add(card.getName());
                enemyCardCount.add(1);
                featureCount+=2;
            }else{
                enemyCardCount.set((enemyCardMap.get(hash)-lastSelfCard-3)/2,2);
            }
        }
        
        for (Card card : opponent.getDeck()){
              int hash = card.getName().hashCode();
            if(!enemyCardMap.containsKey(hash)){
                enemyCardMap.put(hash, featureCount);
                reverseEnemyCardMap.put(featureCount,hash);
                this.enemyCardNames.add(card.getName());
                enemyCardCount.add(1);
                featureCount+=2;
            }else{
                enemyCardCount.set((enemyCardMap.get(hash)-lastSelfCard-3)/2,2);
            }
        }
        lastEnemyCard = featureCount-2;
        //feature data will be features count, my health/armor, opponent health/armor, my board(7*3), opponent board (7*3)
        
        featureData = new double[featureCount + 1 + 1  + 1 + 1 + 7*3 + 7*3];
        this.myBoardStart = featureCount+1+1+1+1;
        this.enemyBoardStart = myBoardStart + 7*3; 
        
     }
    
    public double[] getFeatures(boolean includeAction, GameContext context,Player player){
        //hand,deck,played
        featureData = new double[featureData.length];
        System.err.println("feature count + " + featureCount + " featuredata size" + featureData.length );
        populateFeatureData(this.myCardMap,0,player.getHand().getList());
        populateFeatureData(this.myCardMap,1,player.getDeck().getList());
        
        
        populateFeatureData(this.enemyCardMap,0,context.getOpponent(player).getDeck().getList());
        populateFeatureData(this.enemyCardMap,0,context.getOpponent(player).getHand().getList());
        
        calculatePlayedCards();
        
        
        featureData[featureCount+0] = player.getHero().getArmor()/10.0;
        featureData[featureCount+1] = player.getHero().getHp()/33.0;
        
        Player opponent = context.getOpponent(player);
        
        featureData[featureCount+2] = opponent.getHero().getArmor()/10.0;
        featureData[featureCount+3] = opponent.getHero().getHp()/33.0;
        
        
        addMinions(player.getMinions(), this.myBoardStart);
        addMinions(opponent.getMinions(), this.enemyBoardStart);
        
        return featureData;
    }
    public void addMinions(List<Minion> minions, int indexStart){
        int currentIndex = indexStart;
        for(Minion minion : minions){
            featureData[currentIndex] = minion.getAttack()/12.0;
            featureData[currentIndex+1] = minion.getHp()/12.0;
            double canAttack = .1;
            
            if(minion.canAttackThisTurn())
                canAttack = .9;
            featureData[currentIndex+2] = canAttack;
            
            currentIndex+=3;
            
        }
        
    }
    public void calculatePlayedCards(){
        for(int i = 0; i<=lastSelfCard; i+=3){
            int cardNum = i/3;
            int total = this.cardCount.get(cardNum);
            int cardsPlayed = (int) (total - (featureData[i] + featureData[i+1]));
            featureData[i+2] = cardsPlayed;
        }
        for(int i = lastSelfCard+3; i<=lastEnemyCard; i+=2){
            int cardNum = (i-(lastSelfCard+3))/2;
            int total = this.enemyCardCount.get(cardNum);
            int cardsPlayed = (int) (total - (featureData[i]));
            featureData[i+1] = cardsPlayed;
        }
    }
    public void populateFeatureData(TIntIntHashMap myMap, int offset, List<Card> cards){
        
        for(Card card : cards){
            featureData[myMap.get(card.getName().hashCode())+offset] +=1; 
        }
        
    }
    private FeatureCollector() {}
    
   public void printFeatures(GameContext context,Player player){
       double[] features = this.getFeatures(false, context, player);
       System.err.println("my card data:");
       for(int i = 0; i<this.cardCount.size(); i++){
           String numbers = this.featureData[i*3] + " " + this.featureData[i*3+1] + " " + this.featureData[i*3+2];
           System.err.println(this.cardNames.get(i) + ": " + numbers);
       }
       
       System.err.println("enemy card data:");
        for(int i = 0; i<this.enemyCardCount.size(); i++){
           String numbers = this.featureData[this.lastSelfCard+3 + i*2] + " " + this.featureData[this.lastSelfCard+3 + i*2+1];
           System.err.println(this.enemyCardNames.get(i) + ": " + numbers);
       }
       
        System.err.println("my armor:" + featureData[featureCount]);
        System.err.println("my hp : " + featureData[featureCount+1]);
        
        System.err.println("enemy armor:" + featureData[featureCount+2]);
        System.err.println("enemy hp : " + featureData[featureCount+3]);
        
        
        System.err.println("my board minions:");
        for(int i = this.myBoardStart; i<this.enemyBoardStart; i+=3){
            System.err.println("Minion: " + featureData[i]  + " " + featureData[i+1] + " " + featureData[i+2]);
        }
        
        
        System.err.println("enemy board minions:");
        for(int i = this.enemyBoardStart; i<featureData.length; i+=3){
            System.err.println("eneMinion: " + featureData[i]  + " " + featureData[i+1] + " " + featureData[i+2]);
        }
        
        
   }
}



