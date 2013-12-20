package net.pferdimanzug.hearthstone.analyzer.game.cards.concrete.paladin;

import net.pferdimanzug.hearthstone.analyzer.game.GameTag;
import net.pferdimanzug.hearthstone.analyzer.game.actions.TargetSelection;
import net.pferdimanzug.hearthstone.analyzer.game.cards.Rarity;
import net.pferdimanzug.hearthstone.analyzer.game.cards.SpellCard;
import net.pferdimanzug.hearthstone.analyzer.game.entities.heroes.HeroClass;
import net.pferdimanzug.hearthstone.analyzer.game.spells.BuffSpell;

public class HandOfProtection extends SpellCard {

	public HandOfProtection() {
		super("Hand of Protection", Rarity.FREE, HeroClass.PALADIN, 1);
		setTargetRequirement(TargetSelection.MINIONS);
		setSpell(new BuffSpell(GameTag.DIVINE_SHIELD));
	}

}