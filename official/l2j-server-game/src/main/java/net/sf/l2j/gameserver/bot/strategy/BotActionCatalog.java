/*
 * Copyright © 2026 winterrealm3 contributors
 *
 * This file is part of winterrealm3.
 *
 * winterrealm3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * winterrealm3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable action-node index with complete link validation. */
public final class BotActionCatalog<C>
{
	private final Map<String, BotActionNode<C>> _nodes;

	public BotActionCatalog(Collection<BotActionNode<C>> nodes)
	{
		if (nodes == null)
		{
			throw new IllegalArgumentException("Bot action nodes cannot be null");
		}

		Map<String, BotActionNode<C>> indexedNodes = new LinkedHashMap<>();
		for (BotActionNode<C> node : nodes)
		{
			if (node == null)
			{
				throw new IllegalArgumentException("Bot action nodes cannot contain null");
			}
			BotActionNode<C> existing = indexedNodes.putIfAbsent(node.name(), node);
			if (existing != null)
			{
				throw new IllegalArgumentException("Duplicate bot action: " + node.name());
			}
		}
		for (BotActionNode<C> node : indexedNodes.values())
		{
			validateLinks(node.name(), "prerequisite", node.prerequisites(), indexedNodes);
			validateLinks(node.name(), "alternative", node.alternatives(), indexedNodes);
			validateLinks(node.name(), "continuer", node.continuers(), indexedNodes);
		}
		_nodes = Collections.unmodifiableMap(indexedNodes);
	}

	public BotActionNode<C> get(String actionName)
	{
		return _nodes.get(actionName);
	}

	public int size()
	{
		return _nodes.size();
	}

	private static <C> void validateLinks(String owner, String linkType, List<BotNextAction> links, Map<String, BotActionNode<C>> nodes)
	{
		for (BotNextAction link : links)
		{
			if (!nodes.containsKey(link.actionName()))
			{
				throw new IllegalArgumentException("Bot action " + owner + " has unknown " + linkType + " " + link.actionName());
			}
		}
	}
}
