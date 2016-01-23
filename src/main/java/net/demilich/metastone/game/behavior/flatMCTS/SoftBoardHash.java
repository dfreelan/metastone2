package net.demilich.metastone.game.behavior.flatMCTS;

import net.demilich.metastone.game.behaviour.threat.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.heroes.Hero;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.minions.Minion;

public class SoftBoardHash {

        Random hashGen;
        
	public SoftBoardHash() {
		
	}

	private double calculateMinionScore(Minion minion) {
		if (minion.hasAttribute(Attribute.MARKED_FOR_DEATH)) {
			return hashGen.nextDouble();
		}
		double minionScore = 0.0;
		minionScore += (minion.getAttack() *hashGen.nextDouble());
		minionScore += minion.getHp() *hashGen.nextDouble();
                
		if (minion.hasAttribute(Attribute.WINDFURY)) {
			minionScore += hashGen.nextDouble();
		} else if (minion.hasAttribute(Attribute.MEGA_WINDFURY)) {
			minionScore += hashGen.nextDouble();
		}

		if (minion.hasAttribute(Attribute.DIVINE_SHIELD)) {
			minionScore += hashGen.nextDouble()*.001;
		}
		if (minion.hasAttribute(Attribute.SPELL_DAMAGE)) {
			minionScore += minion.getAttributeValue(Attribute.SPELL_DAMAGE)*hashGen.nextDouble()*.1;
                }

		if (minion.hasAttribute(Attribute.STEALTH)) {
			minionScore += hashGen.nextDouble()*.01;
		}
		if (minion.hasAttribute(Attribute.UNTARGETABLE_BY_SPELLS)) {
			minionScore += hashGen.nextDouble()*.002;
		}

		return minionScore;
	}

	
	public double getScore(GameContext context, int playerId) {
		hashGen = new Random(63484472);
                Player player = context.getPlayer(playerId);
		Player opponent = context.getOpponent(player);
		
		double score = 0;

		
		score += player.getHero().getEffectiveHp()*hashGen.nextDouble()*8;
		score += opponent.getHero().getEffectiveHp()*hashGen.nextDouble()*-4;
		for (Card card : player.getHand()) {
			score+= hashGen.nextDouble()*card.getName().charAt(0)*card.getName().length();
		}
                for (Card card : opponent.getHand()) {
			score+=hashGen.nextDouble()*card.getName().charAt(0)*card.getName().length();
		}

		for (Minion minion : player.getMinions()) {
			score += calculateMinionScore(minion);
		}

		for (Minion minion : opponent.getMinions()) {
			score -= calculateMinionScore(minion);
		}

		return score;
	}



}
