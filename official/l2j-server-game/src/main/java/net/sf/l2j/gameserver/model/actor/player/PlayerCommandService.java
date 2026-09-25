/*
 * Copyright © 2004-2020 L2J Server
 * Copyright © 2026 winterrealm3 contributors
 *
 * This file is part of winterrealm3 and is derived from L2J Server.
 * It was modified by winterrealm3 contributors in 2026.
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
package net.sf.l2j.gameserver.model.actor.player;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.templates.L2WeaponType;

/**
 * Client-independent command boundary shared by packet handlers and native bot
 * actuators. Low-level execution remains owned by the normal player AI.
 */
public final class PlayerCommandService
{
	private static final double MAXIMUM_MOVE_DISTANCE_SQUARED = 98010000.0;

	private PlayerCommandService()
	{
	}

	public static PlayerCommandService getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Requests the normal primary interaction for an object.
	 * @param player the acting player
	 * @param target the target object
	 * @return the semantic command outcome
	 */
	public PlayerCommandResult interact(L2PcInstance player, L2Object target)
	{
		if (player == null)
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		if (target == null)
		{
			return PlayerCommandResult.TARGET_UNAVAILABLE;
		}
		if ((player.getPrivateStoreType() != 0) || (player.getActiveRequester() != null))
		{
			return PlayerCommandResult.ACTION_BLOCKED;
		}

		target.onAction(player);
		return PlayerCommandResult.ACCEPTED;
	}

	/**
	 * Selects an object through its normal first-click validation without
	 * triggering second-click interaction when it is already selected.
	 * @param player the selecting player
	 * @param target the requested target
	 * @return the semantic command outcome
	 */
	public PlayerCommandResult selectTarget(L2PcInstance player, L2Object target)
	{
		if (player == null)
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		if (target == null)
		{
			return PlayerCommandResult.TARGET_UNAVAILABLE;
		}
		if (player.getTarget() == target)
		{
			return PlayerCommandResult.ACCEPTED;
		}
		if ((player.getPrivateStoreType() != 0) || (player.getActiveRequester() != null))
		{
			return PlayerCommandResult.ACTION_BLOCKED;
		}

		target.onAction(player);
		return player.getTarget() == target ? PlayerCommandResult.TARGET_SELECTED : PlayerCommandResult.TARGET_REJECTED;
	}

	/**
	 * Requests the same two-stage target/forced-attack behavior used by clients.
	 * @param player the attacking player
	 * @param target the requested target
	 * @return the semantic command outcome
	 */
	public PlayerCommandResult attack(L2PcInstance player, L2Object target)
	{
		if (player == null)
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		if (target == null)
		{
			return PlayerCommandResult.TARGET_UNAVAILABLE;
		}
		if (player.getTarget() != target)
		{
			target.onAction(player);
			return PlayerCommandResult.TARGET_SELECTED;
		}
		if (target.getObjectId() == player.getObjectId())
		{
			return PlayerCommandResult.SELF_TARGET;
		}
		if ((player.getPrivateStoreType() != 0) || (player.getActiveRequester() != null))
		{
			return PlayerCommandResult.ACTION_BLOCKED;
		}

		target.onForcedAttack(player);
		return PlayerCommandResult.ACCEPTED;
	}

	/**
	 * Resolves and requests a skill the player actually owns. Detailed casting
	 * validation and execution continue through {@link L2PcInstance#useMagic}.
	 * @param player the casting player
	 * @param skillId the owned skill identifier
	 * @param forceUse whether offensive use should be forced
	 * @param dontMove whether casting must not approach an out-of-range target
	 * @return the semantic command outcome
	 */
	public PlayerCommandResult useSkill(L2PcInstance player, int skillId, boolean forceUse, boolean dontMove)
	{
		if (player == null)
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}

		int level = player.getSkillLevel(skillId);
		if (level <= 0)
		{
			return PlayerCommandResult.SKILL_NOT_KNOWN;
		}
		if (player.isOutOfControl())
		{
			return PlayerCommandResult.OUT_OF_CONTROL;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		if (skill == null)
		{
			return PlayerCommandResult.SKILL_NOT_FOUND;
		}
		if ((skill.getSkillType() == L2Skill.SkillType.RECALL) && !Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (player.getKarma() > 0))
		{
			return PlayerCommandResult.RECALL_RESTRICTED;
		}

		player.useMagic(skill, forceUse, dontMove);
		return PlayerCommandResult.ACCEPTED;
	}

	/**
	 * Requests normal validated land movement through {@code L2PlayerAI}.
	 * @param player the moving player
	 * @param targetX destination x coordinate
	 * @param targetY destination y coordinate
	 * @param targetZ destination z coordinate
	 * @return the semantic command outcome
	 */
	public PlayerCommandResult moveTo(L2PcInstance player, int targetX, int targetY, int targetZ)
	{
		if (player == null)
		{
			return PlayerCommandResult.PLAYER_UNAVAILABLE;
		}
		if (player.isInBoat())
		{
			player.setInBoat(false);
		}
		if (player.isAttackingNow() && (player.getActiveWeaponItem() != null) && (player.getActiveWeaponItem().getItemType() == L2WeaponType.BOW))
		{
			return PlayerCommandResult.MOVEMENT_BLOCKED;
		}
		if (player.isOutOfControl())
		{
			return PlayerCommandResult.OUT_OF_CONTROL;
		}

		double dx = targetX - player.getX();
		double dy = targetY - player.getY();
		if (((dx * dx) + (dy * dy)) > MAXIMUM_MOVE_DISTANCE_SQUARED)
		{
			return PlayerCommandResult.DESTINATION_TOO_FAR;
		}

		player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(targetX, targetY, targetZ, 0));
		if (player.getParty() != null)
		{
			player.getParty().broadcastToPartyMembers(player, new PartyMemberPosition(player));
		}
		return PlayerCommandResult.ACCEPTED;
	}

	private static final class SingletonHolder
	{
		private static final PlayerCommandService INSTANCE = new PlayerCommandService();
	}
}
