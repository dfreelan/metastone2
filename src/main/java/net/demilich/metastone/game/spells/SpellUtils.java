package net.demilich.metastone.game.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.Environment;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.DiscoverAction;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardCollection;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.EntityType;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.entities.minions.Race;
import net.demilich.metastone.game.spells.desc.SpellArg;
import net.demilich.metastone.game.spells.desc.SpellDesc;
import net.demilich.metastone.game.spells.desc.filter.EntityFilter;
import net.demilich.metastone.game.spells.desc.filter.Operation;
import net.demilich.metastone.game.targeting.EntityReference;

public class SpellUtils {

	public static void castChildSpell(GameContext context, Player player, SpellDesc spell, Entity source, Entity target) {
		EntityReference sourceReference = source != null ? source.getReference() : null;
		EntityReference targetReference = spell.getTarget();
		if (targetReference == null && target != null) {
			targetReference = target.getReference();
		}
		context.getLogic().castSpell(player.getId(), spell, sourceReference, targetReference, true);
	}

	public static boolean evaluateOperation(Operation operation, int actualValue, int targetValue) {
		switch (operation) {
		case EQUAL:
			return actualValue == targetValue;
		case GREATER:
			return actualValue > targetValue;
		case HAS:
			return actualValue > 0;
		case LESS:
			return actualValue < targetValue;
		}
		return false;
	}

	public static CardCollection getCards(CardCollection source, Predicate<Card> filter) {
		CardCollection result = new CardCollection();
		for (Card card : source) {
			if (filter.test(card)) {
				result.add(card);
			}
		}
		return result;
	}
	
	public static Card getCard(GameContext context, SpellDesc spell) {
		Card card = null;
		String cardName = (String) spell.get(SpellArg.CARD);
		card = CardCatalogue.getCardById(cardName);
		if (spell.get(SpellArg.CARD).toString().equals("PENDING_CARD")) {
			card = (Card) context.getEnvironment().get(Environment.PENDING_CARD);
		}
		return card;
	}

	public static Card[] getCards(SpellDesc spell) {
		String[] cardNames = null;
		if (spell.contains(SpellArg.CARDS)) {
			cardNames = (String[]) spell.get(SpellArg.CARDS);
		} else {
			cardNames = new String[1];
			cardNames[0] = (String) spell.get(SpellArg.CARD);
		}
		Card[] cards = new Card[cardNames.length];
		for (int i = 0; i < cards.length; i++) {
			cards[i] = CardCatalogue.getCardById(cardNames[i]);
		}
		return cards;
	}
	
	public static DiscoverAction getDiscover(GameContext context, Player player, SpellDesc desc, CardCollection cards) {
		SpellDesc spell = (SpellDesc) desc.get(SpellArg.SPELL_1);
		List<GameAction> discoverActions = new ArrayList<>();
		for (Card card : cards) {
			SpellDesc spellClone = spell.addArg(SpellArg.CARD, card.getCardId());
			DiscoverAction discover = DiscoverAction.createDiscover(spellClone);
			discover.setActionSuffix(card.getName());
			discoverActions.add(discover);
		}
		
		return (DiscoverAction) player.getBehaviour().requestAction(context, player, discoverActions);
	}

	public static Card getRandomCard(CardCollection source, Predicate<Card> filter) {
		CardCollection result = getCards(source, filter);
		if (result.isEmpty()) {
			return null;
		}
		return result.getRandom();
	}
	
	public static HeroClass getRandomHeroClass() {
		HeroClass randomClass = HeroClass.ANY;
		HeroClass[] values = HeroClass.values();
		while (!isBaseClass(randomClass)) {
			randomClass = values[ThreadLocalRandom.current().nextInt(values.length)];
		}
		return randomClass;
	}

	public static <T> T getRandomTarget(List<T> targets) {
		int randomIndex = ThreadLocalRandom.current().nextInt(targets.size());
		return targets.get(randomIndex);
	}

	public static List<Actor> getValidRandomTargets(List<Entity> targets) {
		List<Actor> validTargets = new ArrayList<Actor>();
		for (Entity entity : targets) {
			Actor actor = (Actor) entity;
			if (!actor.isDestroyed() || actor.getEntityType() == EntityType.HERO) {
				validTargets.add(actor);
			}

		}
		return validTargets;
	}

	public static List<Entity> getValidTargets(GameContext context, Player player, List<Entity> allTargets, EntityFilter filter) {
		if (filter == null) {
			return allTargets;
		}
		List<Entity> validTargets = new ArrayList<>();
		for (Entity entity : allTargets) {
			if (filter.matches(context, player, entity)) {
				validTargets.add(entity);
			}
		}
		return validTargets;
	}

	public static int hasHowManyOfRace(Player player, Race race) {
		int count = 0;
		for (Minion minion : player.getMinions()) {
			if (minion.getRace() == race) {
				count++;
			}
		}
		return count;
	}
	
	public static boolean highlanderDeck(Player player) {
		List<String> cards = new ArrayList<String>();
		for (Card card : player.getDeck()) {
			if (cards.contains(card.getCardId())) {
				return false;
			}
			cards.add(card.getCardId());
		}
		return true;
	}

	public static boolean holdsCardOfType(Player player, CardType cardType) {
		for (Card card : player.getHand()) {
			if (card.getCardType() == cardType) {
				return true;
			}
		}
		return false;
	}

	public static boolean holdsMinionOfRace(Player player, Race race) {
		for (Card card : player.getHand()) {
			if (card.getAttribute(Attribute.RACE) == race) {
				return true;
			}
		}
		return false;
	}

	public static int howManyMinionsDiedThisTurn(GameContext context) {
		int currentTurn = context.getTurn();
		int count = 0;
		for (Player player : context.getPlayers()) {
			for (Entity deadEntity : player.getGraveyard()) {
				if (deadEntity.getEntityType() != EntityType.MINION) {
					continue;
				}

				if (deadEntity.getAttributeValue(Attribute.DIED_ON_TURN) == currentTurn) {
					count++;
				}

			}
		}
		return count;
	}
	
	public static boolean isBaseClass(HeroClass heroClass) {
		if (heroClass == HeroClass.ANY || heroClass == HeroClass.DECK_COLLECTION || heroClass == HeroClass.OPPONENT || heroClass == HeroClass.BOSS) {
			return false;
		}
		return true;
	}

	private SpellUtils() {
	}

}
