/*
 * Copyright © 2004-2020 L2J Server
 *
 * This file is part of L2J Server.
 *
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.profile;

import java.util.List;
import java.util.Optional;

import net.sf.l2j.gameserver.bot.perception.BotObservedCharacter;
import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.GroupFilter;

/** Evaluates exact-class policy conditions using only immutable perception. */
public final class BotConditionEvaluator
{
	public boolean matches(List<BotRuleCondition> conditions, BotPerceptionSnapshot snapshot, int resolvedTargetObjectId)
	{
		if ((conditions == null) || (snapshot == null))
		{
			throw new IllegalArgumentException("Condition evaluation requires conditions and a snapshot");
		}
		for (BotRuleCondition condition : conditions)
		{
			if ((condition == null) || !matches(condition, snapshot, resolvedTargetObjectId))
			{
				return false;
			}
		}
		return true;
	}

	private boolean matches(BotRuleCondition condition, BotPerceptionSnapshot snapshot, int targetObjectId)
	{
		if (condition instanceof BotRuleCondition.Resource resource)
		{
			return snapshot.subject(resource.subject(), targetObjectId).map(character -> resource.comparison().test(character.resource(resource.resource(), resource.amount()), resource.value())).orElse(false);
		}
		if (condition instanceof BotRuleCondition.Distance distance)
		{
			return snapshot.subject(distance.subject(), targetObjectId).map(character -> distance.comparison().test(character.distance(), distance.value())).orElse(false);
		}
		if (condition instanceof BotRuleCondition.Effect effect)
		{
			return snapshot.subject(effect.subject(), targetObjectId).map(character -> character.hasEffect(effect.skillId()) == (effect.state() == BotRuleCondition.EffectState.PRESENT)).orElse(false);
		}
		if (condition instanceof BotRuleCondition.Status status)
		{
			return snapshot.subject(status.subject(), targetObjectId).map(character -> character.status(status.status()) == status.expected()).orElse(false);
		}
		if (condition instanceof BotRuleCondition.ExactClass exactClass)
		{
			return snapshot.subject(exactClass.subject(), targetObjectId).map(character -> (character.activeClass() == exactClass.classId()) == exactClass.expected()).orElse(false);
		}
		if (condition instanceof BotRuleCondition.GroupCount count)
		{
			long actual = snapshot.group(count.group()).stream().map(snapshot::find).flatMap(Optional::stream).filter(character -> character.distance() <= count.radius()).filter(character -> matchesFilter(character, count.filter(), count.threshold())).count();
			return count.comparison().test(actual, count.count());
		}
		if (condition instanceof BotRuleCondition.GroupClassCount count)
		{
			long actual = snapshot.group(count.group()).stream().map(snapshot::find).flatMap(Optional::stream).filter(BotObservedCharacter::alive).filter(character -> character.distance() <= count.radius()).filter(character -> character.activeClass() == count.classId()).filter(character -> character.hasEffect(count.effectSkillId()) == (count.effectState() == BotRuleCondition.EffectState.PRESENT)).count();
			return count.comparison().test(actual, count.count());
		}
		throw new IllegalStateException("Unhandled bot rule condition " + condition.getClass().getName());
	}

	private static boolean matchesFilter(BotObservedCharacter character, GroupFilter filter, int threshold)
	{
		return switch (filter)
		{
			case ALL -> true;
			case ALIVE -> character.alive();
			case DEAD -> !character.alive();
			case HP_PERCENT_BELOW -> character.alive() && character.resource(BotRuleCondition.ResourceType.HP, BotRuleCondition.AmountType.PERCENT) < threshold;
			case MP_PERCENT_BELOW -> character.alive() && character.resource(BotRuleCondition.ResourceType.MP, BotRuleCondition.AmountType.PERCENT) < threshold;
			case CP_PERCENT_BELOW -> character.alive() && character.maximumCp() > 0 && character.resource(BotRuleCondition.ResourceType.CP, BotRuleCondition.AmountType.PERCENT) < threshold;
		};
	}
}
