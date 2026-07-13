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
package net.sf.l2j.gameserver.bot.tactics;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import net.sf.l2j.gameserver.bot.profile.BotClassProfile;
import net.sf.l2j.gameserver.bot.strategy.BotNextAction;
import net.sf.l2j.gameserver.bot.strategy.BotStrategy;
import net.sf.l2j.gameserver.model.base.ClassId;

/** Queues only actions declared by one exact ClassId/profile-name policy. */
final class BotExactClassStrategy implements BotStrategy<BotDecisionContext>
{
	private final String _name;
	private final List<BotContextualNextAction> _unindexedActions;
	private final Map<ClassId, List<BotContextualNextAction>> _actionsByObservedClass;
	private final List<BotContextualNextAction> _multiClassActions;

	BotExactClassStrategy(BotClassProfile profile, List<BotContextualNextAction> actions)
	{
		if ((profile == null) || (actions == null))
		{
			throw new IllegalArgumentException("Exact class strategy requires profile and actions");
		}
		_name = "exact-class:" + profile.getClassId() + ":" + profile.getProfileName();

		List<BotContextualNextAction> unindexed = new ArrayList<>();
		EnumMap<ClassId, List<BotContextualNextAction>> indexed = new EnumMap<>(ClassId.class);
		List<BotContextualNextAction> multiClass = new ArrayList<>();
		for (BotContextualNextAction action : actions)
		{
			if (action == null)
			{
				throw new IllegalArgumentException("Exact class strategy actions cannot contain null");
			}
			if (action.requiredObservedClasses().isEmpty())
			{
				unindexed.add(action);
			}
			else if (action.requiredObservedClasses().size() == 1)
			{
				ClassId classId = action.requiredObservedClasses().iterator().next();
				indexed.computeIfAbsent(classId, ignored -> new ArrayList<>()).add(action);
			}
			else
			{
				multiClass.add(action);
			}
		}
		_unindexedActions = List.copyOf(unindexed);
		EnumMap<ClassId, List<BotContextualNextAction>> immutableIndex = new EnumMap<>(ClassId.class);
		indexed.forEach((classId, classActions) -> immutableIndex.put(classId, List.copyOf(classActions)));
		_actionsByObservedClass = Map.copyOf(immutableIndex);
		_multiClassActions = List.copyOf(multiClass);
	}

	@Override
	public String name()
	{
		return _name;
	}

	@Override
	public List<BotNextAction> defaultActions(BotDecisionContext context)
	{
		EnumSet<ClassId> observedClasses = EnumSet.noneOf(ClassId.class);
		context.perception().characters().stream().map(character -> character.activeClass()).filter(classId -> classId != null).forEach(observedClasses::add);

		List<BotNextAction> useful = new ArrayList<>();
		addUseful(_unindexedActions, context, useful);
		for (ClassId classId : ClassId.values())
		{
			if (observedClasses.contains(classId))
			{
				addUseful(_actionsByObservedClass.getOrDefault(classId, List.of()), context, useful);
			}
		}
		for (BotContextualNextAction action : _multiClassActions)
		{
			if (observedClasses.containsAll(action.requiredObservedClasses()) && action.action().isUseful(context))
			{
				useful.add(action.next());
			}
		}
		return List.copyOf(useful);
	}

	private static void addUseful(List<BotContextualNextAction> actions, BotDecisionContext context, List<BotNextAction> useful)
	{
		for (BotContextualNextAction action : actions)
		{
			if (action.action().isUseful(context))
			{
				useful.add(action.next());
			}
		}
	}
}
