package net.pferdimanzug.hearthstone.analyzer.game.cards.concrete.neutral;

import net.pferdimanzug.hearthstone.analyzer.game.actions.Battlecry;
import net.pferdimanzug.hearthstone.analyzer.game.cards.MinionCard;
import net.pferdimanzug.hearthstone.analyzer.game.cards.Rarity;
import net.pferdimanzug.hearthstone.analyzer.game.entities.heroes.HeroClass;
import net.pferdimanzug.hearthstone.analyzer.game.entities.minions.Minion;
import net.pferdimanzug.hearthstone.analyzer.game.entities.minions.Race;
import net.pferdimanzug.hearthstone.analyzer.game.spells.DestroySpell;
import net.pferdimanzug.hearthstone.analyzer.game.spells.DiscardCardSpell;
import net.pferdimanzug.hearthstone.analyzer.game.spells.MetaSpell;
import net.pferdimanzug.hearthstone.analyzer.game.spells.Spell;
import net.pferdimanzug.hearthstone.analyzer.game.targeting.EntityReference;

public class Deathwing extends MinionCard {

	public Deathwing() {
		super("Deathwing", 12, 12, Rarity.LEGENDARY, HeroClass.ANY, 10);
		setDescription("Battlecry: Destroy all other minions and discard your hand.");
		setRace(Race.DRAGON);
	}

	@Override
	public Minion summon() {
		Minion deathwing = createMinion();
		Spell destroySpell = new DestroySpell();
		destroySpell.setTarget(EntityReference.ALL_MINIONS);
		Spell discardSpell = new DiscardCardSpell(DiscardCardSpell.ALL_CARDS);
		Battlecry battlecry = Battlecry.createBattlecry(new MetaSpell(destroySpell, discardSpell));
		deathwing.setBattlecry(battlecry);
		return deathwing;
	}

}