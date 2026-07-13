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

import net.sf.l2j.gameserver.bot.BotRuntime;
import net.sf.l2j.gameserver.bot.BotRuntimeState;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.player.PlayerCommandResult;
import net.sf.l2j.gameserver.model.actor.player.PlayerCommandService;

/** Resolves snapshot object IDs at execution time and invokes shared commands. */
public final class BotLiveActionExecutor implements BotActionExecutor
{
	private final BotRuntime _runtime;
	private final PlayerCommandService _commands;

	public BotLiveActionExecutor(BotRuntime runtime)
	{
		if (runtime == null)
		{
			throw new IllegalArgumentException("Bot action executor runtime cannot be null");
		}
		_runtime = runtime;
		_commands = PlayerCommandService.getInstance();
	}

	@Override
	public boolean isActive()
	{
		return _runtime.getState() == BotRuntimeState.ACTIVE;
	}

	@Override
	public boolean knowsSkill(int skillId)
	{
		return _runtime.getPlayer().getSkillLevel(skillId) > 0;
	}

	@Override
	public boolean isSkillReady(int skillId)
	{
		L2PcInstance player = _runtime.getPlayer();
		int level = player.getSkillLevel(skillId);
		if ((level <= 0) || player.isSkillDisabled(skillId) || player.isAllSkillsDisabled())
		{
			return false;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		if ((skill == null) || skill.isPassive())
		{
			return false;
		}
		if (player.getCurrentMp() < (player.getStat().getMpConsume(skill) + player.getStat().getMpInitialConsume(skill)))
		{
			return false;
		}
		if (player.getCurrentHp() <= skill.getHpConsume())
		{
			return false;
		}
		if (skill.getItemConsume() <= 0)
		{
			return true;
		}

		var item = player.getInventory().getItemByItemId(skill.getItemConsumeId());
		return (item != null) && (item.getCount() >= skill.getItemConsume());
	}

	@Override
	public PlayerCommandResult selectTarget(int targetObjectId)
	{
		if (!isActive())
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		return _commands.selectTarget(_runtime.getPlayer(), L2World.getInstance().findObject(targetObjectId));
	}

	@Override
	public PlayerCommandResult castSkill(BotSkillPolicy policy, int targetObjectId)
	{
		if (!isActive())
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		if (policy == null)
		{
			throw new IllegalArgumentException("Bot cast policy cannot be null");
		}
		L2PcInstance player = _runtime.getPlayer();
		L2Object target = L2World.getInstance().findObject(targetObjectId);
		if (target == null)
		{
			return PlayerCommandResult.TARGET_UNAVAILABLE;
		}
		if (player.getTarget() != target)
		{
			return PlayerCommandResult.TARGET_REJECTED;
		}
		return _commands.useSkill(player, policy.skillId(), policy.forceUse(), policy.dontMove());
	}

	@Override
	public PlayerCommandResult attack(int targetObjectId)
	{
		if (!isActive())
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		L2PcInstance player = _runtime.getPlayer();
		L2Object target = L2World.getInstance().findObject(targetObjectId);
		if (target == null)
		{
			return PlayerCommandResult.TARGET_UNAVAILABLE;
		}
		if (player.getTarget() != target)
		{
			return PlayerCommandResult.TARGET_REJECTED;
		}
		return _commands.attack(player, target);
	}
}
