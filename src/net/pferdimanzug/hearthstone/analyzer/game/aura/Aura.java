package net.pferdimanzug.hearthstone.analyzer.game.aura;

import java.util.HashSet;
import java.util.List;

import net.pferdimanzug.hearthstone.analyzer.game.GameContext;
import net.pferdimanzug.hearthstone.analyzer.game.Player;
import net.pferdimanzug.hearthstone.analyzer.game.entities.Actor;
import net.pferdimanzug.hearthstone.analyzer.game.entities.Entity;
import net.pferdimanzug.hearthstone.analyzer.game.entities.EntityType;
import net.pferdimanzug.hearthstone.analyzer.game.events.GameEvent;
import net.pferdimanzug.hearthstone.analyzer.game.spells.Spell;
import net.pferdimanzug.hearthstone.analyzer.game.spells.trigger.BoardChangedTrigger;
import net.pferdimanzug.hearthstone.analyzer.game.spells.trigger.SpellTrigger;
import net.pferdimanzug.hearthstone.analyzer.game.targeting.EntityReference;

public class Aura extends SpellTrigger {

	private EntityReference targets;
	private Spell applyAuraEffect;
	private Spell removeAuraEffect;
	private final HashSet<Integer> affectedEntities = new HashSet<>();

	public Aura(Spell applyAuraEffect, Spell removeAuraEffect, EntityReference targetSelection) {
		super(new BoardChangedTrigger(), applyAuraEffect);
		this.applyAuraEffect = applyAuraEffect;
		this.removeAuraEffect = removeAuraEffect;
		this.targets = targetSelection;
	}
	
	protected boolean affects(GameContext context, Entity target) {
		if (target.getEntityType() != EntityType.MINION) {
			System.out.println("Entity not minion");
			return false;
		}
		if (target.getReference().equals(getHostReference())) {
			System.out.println("Entity is HOST");
			return false;
		}

		Actor targetActor = (Actor) target;
		if (targetActor.isDead()) {
			System.out.println("Entity is DEAD");
			return false;
		}

		System.out.println("Entity is affected!");
		return true;
	}
	
	@Override
	public Aura clone() {
		Aura clone = (Aura) super.clone();
		clone.targets = this.targets;
		clone.applyAuraEffect = this.applyAuraEffect.clone();
		clone.removeAuraEffect = this.removeAuraEffect.clone();
		affectedEntities.addAll(this.affectedEntities);
		return clone;
	}

	public void onGameEvent(GameEvent event) {
		
		GameContext context = event.getGameContext();
		Player owner = context.getPlayer(getOwner());
		Actor sourceActor = (Actor) context.resolveSingleTarget(getOwner(), getHostReference());
		System.out.println("Event received for " + sourceActor);
		List<Entity> resolvedTargets = context.resolveTarget(owner, sourceActor, targets);

		for (Entity target : resolvedTargets) {
			System.out.println("Aura of " + sourceActor + " is checking potential target " + target);
			if (!affects(context, target) && !affectedEntities.contains(target.getId())) {
				continue;
			} else if (affects(context, target) && !affectedEntities.contains(target.getId())) {
				applyAuraEffect.setTarget(target.getReference());
				context.getLogic().castSpell(getOwner(), applyAuraEffect);
				affectedEntities.add(target.getId());
			} else if (!affects(context, target) && affectedEntities.contains(target.getId())) {
				removeAuraEffect.setTarget(target.getReference());
				context.getLogic().castSpell(getOwner(), removeAuraEffect);
				affectedEntities.remove(target.getId());
			}
		}

	}

	@Override
	public void onRemove(GameContext context) {
		for (int targetId : affectedEntities) {
			EntityReference targetKey = new EntityReference(targetId);
			Entity target = context.resolveSingleTarget(getOwner(), targetKey);
			removeAuraEffect.setTarget(target.getReference());
			context.getLogic().castSpell(getOwner(), removeAuraEffect);
		}
		affectedEntities.clear();
	}

}
