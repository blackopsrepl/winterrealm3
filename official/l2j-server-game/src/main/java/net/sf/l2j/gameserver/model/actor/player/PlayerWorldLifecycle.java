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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.FriendList;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * Owns client-independent player entry and exit behavior shared by human and
 * native bot controllers. Packet presentation remains at the network edge.
 */
public final class PlayerWorldLifecycle
{
	private static final Logger _log = Logger.getLogger(PlayerWorldLifecycle.class.getName());

	private PlayerWorldLifecycle()
	{
	}

	/**
	 * Gets the shared lifecycle service.
	 * @return the lifecycle service
	 */
	public static PlayerWorldLifecycle getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Applies state required immediately before a player is spawned.
	 * @param player the entering player
	 */
	public void prepareSpawn(L2PcInstance player)
	{
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			player.setProtection(true);
		}
	}

	/**
	 * Spawns a player and restores event location state.
	 * @param player the entering player
	 */
	public void spawn(L2PcInstance player)
	{
		player.spawnMe(player.getX(), player.getY(), player.getZ());
		if (L2Event.active && L2Event.connectionLossData.containsKey(player.getName()) && L2Event.isOnEvent(player))
		{
			L2Event.restoreChar(player);
		}
		else if (L2Event.connectionLossData.containsKey(player.getName()))
		{
			L2Event.restoreAndTeleChar(player);
		}
	}

	/**
	 * Restores persisted effects at the same lifecycle point used by clients.
	 * @param player the entering player
	 */
	public void restoreEffects(L2PcInstance player)
	{
		if (Config.STORE_SKILL_COOLTIME)
		{
			player.restoreEffects();
		}
	}

	/**
	 * Restores configured couple state and notifies an online partner.
	 * @param player the entering player
	 */
	public void restoreRelationshipState(L2PcInstance player)
	{
		if (!Config.L2JMOD_ALLOW_WEDDING)
		{
			return;
		}

		int characterId = player.getObjectId();
		for (Couple couple : CoupleManager.getInstance().getCouples())
		{
			if ((couple.getPlayer1Id() != characterId) && (couple.getPlayer2Id() != characterId))
			{
				continue;
			}
			if (couple.getMaried())
			{
				player.setMarried(true);
			}
			player.setCoupleId(couple.getId());
			player.setPartnerId((couple.getPlayer1Id() == characterId) ? couple.getPlayer2Id() : couple.getPlayer1Id());
		}

		if (player.getPartnerId() != 0)
		{
			L2PcInstance partner = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());
			if (partner != null)
			{
				partner.sendMessage("Your Partner has logged in");
			}
		}
	}

	/**
	 * Removes non-restorable regeneration effects and reapplies equipped item
	 * augmentations.
	 * @param player the entering player
	 */
	public void finishPersistentRestore(L2PcInstance player)
	{
		if (player.getAllEffects() != null)
		{
			for (L2Effect effect : player.getAllEffects())
			{
				if (effect.getEffectType() == L2Effect.EffectType.HEAL_OVER_TIME)
				{
					player.stopEffects(L2Effect.EffectType.HEAL_OVER_TIME);
					player.removeEffect(effect);
				}
				if (effect.getEffectType() == L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME)
				{
					player.stopEffects(L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME);
					player.removeEffect(effect);
				}
			}
		}

		for (L2ItemInstance item : player.getInventory().getAugmentedItems())
		{
			if ((item != null) && item.isEquipped())
			{
				item.getAugmentation().applyBoni(player);
			}
		}
	}

	/**
	 * Restores the player's quest states through the normal quest engine.
	 * @param player the entering player
	 */
	public void restoreQuestState(L2PcInstance player)
	{
		Quest.playerEnter(player);
	}

	/**
	 * Refreshes environment, hero, and pledge state before marking the player
	 * online.
	 * @param player the entering player
	 */
	public void prepareActivation(L2PcInstance player)
	{
		if (Config.ALLOW_WATER)
		{
			player.checkWaterState();
		}
		if ((Hero.getInstance().getHeroes() != null) && Hero.getInstance().getHeroes().containsKey(player.getObjectId()))
		{
			player.setHero(true);
		}
		setPledgeClass(player);
	}

	/**
	 * Marks a player online and publishes normal friend, clan, and academy
	 * presence before running player-entry domain checks.
	 * @param player the entering player
	 */
	public void activate(L2PcInstance player)
	{
		player.setOnlineStatus(true);
		notifyFriends(player);
		notifyClanMembers(player);
		notifySponsorOrApprentice(player);
		player.onPlayerEnter();
	}

	/**
	 * Removes players from persisted locations that cannot survive a login.
	 * @param player the entering player
	 */
	public void enforceRestrictedLocations(L2PcInstance player)
	{
		if (Olympiad.getInstance().playerInStadia(player))
		{
			player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			player.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium");
		}
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
		}
	}

	/**
	 * Recalculates current siege participation for a clan member.
	 * @param player the entering player
	 */
	public void refreshSiegeState(L2PcInstance player)
	{
		if (player.getClan() == null)
		{
			return;
		}
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (!siege.getIsInProgress())
			{
				continue;
			}
			if (siege.checkIsAttacker(player.getClan()))
			{
				player.setSiegeState((byte) 1);
			}
			else if (siege.checkIsDefender(player.getClan()))
			{
				player.setSiegeState((byte) 2);
			}
		}
	}

	/**
	 * Enforces the normal post-login siege-zone relocation rule.
	 * @param player the entering player
	 */
	public void enforceSiegeZone(L2PcInstance player)
	{
		if (!player.isGM() && (player.getSiegeState() < 2) && player.isInsideZone(L2Character.ZONE_SIEGE))
		{
			player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			player.sendMessage("You have been teleported to the nearest town due to you being in siege zone");
		}
	}

	/**
	 * Completes entry hooks that depend on a fully active world player.
	 * @param player the entering player
	 */
	public void completeEntry(L2PcInstance player)
	{
		RegionBBSManager.getInstance().changeCommunityBoard();
		TvTEvent.onLogin(player);
	}

	/**
	 * Runs the complete shared entry sequence for a clientless bot player.
	 * @param player the bot-controlled player
	 */
	public void enterClientlessPlayer(L2PcInstance player)
	{
		if ((player == null) || !player.isBotControlled())
		{
			throw new IllegalArgumentException("Clientless world entry requires a bot-controlled player");
		}
		if ((L2World.getInstance().findObject(player.getObjectId()) != null) || (L2World.getInstance().getPlayer(player.getName()) != null))
		{
			throw new IllegalStateException("Player is already present in the world: " + player.getName());
		}

		prepareSpawn(player);
		spawn(player);
		restoreEffects(player);
		restoreRelationshipState(player);
		finishPersistentRestore(player);
		restoreQuestState(player);
		prepareActivation(player);
		activate(player);
		enforceRestrictedLocations(player);
		refreshSiegeState(player);
		enforceSiegeZone(player);
		completeEntry(player);
	}

	/**
	 * Stores and removes a clientless player through normal player teardown.
	 * @param player the bot-controlled player
	 */
	public void leaveClientlessPlayer(L2PcInstance player)
	{
		if (player == null)
		{
			return;
		}

		RuntimeException failure = null;
		try
		{
			TvTEvent.onLogout(player);
		}
		catch (RuntimeException e)
		{
			failure = e;
		}
		try
		{
			RegionBBSManager.getInstance().changeCommunityBoard();
		}
		catch (RuntimeException e)
		{
			failure = accumulate(failure, e);
		}
		try
		{
			player.deleteMe();
		}
		catch (RuntimeException e)
		{
			failure = accumulate(failure, e);
		}
		try
		{
			player.store();
		}
		catch (RuntimeException e)
		{
			failure = accumulate(failure, e);
		}
		if (failure != null)
		{
			throw failure;
		}
	}

	private static RuntimeException accumulate(RuntimeException failure, RuntimeException next)
	{
		if (failure == null)
		{
			return next;
		}
		failure.addSuppressed(next);
		return failure;
	}

	private static void notifyFriends(L2PcInstance player)
	{
		try (Connection connection = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT friend_name FROM character_friends WHERE char_id=?"))
		{
			statement.setInt(1, player.getObjectId());
			try (ResultSet result = statement.executeQuery())
			{
				SystemMessage message = new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
				message.addString(player.getName());
				while (result.next())
				{
					L2PcInstance friend = L2World.getInstance().getPlayer(result.getString("friend_name"));
					if (friend != null)
					{
						friend.sendPacket(new FriendList(friend));
						friend.sendPacket(message);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not restore friend data: " + e);
		}
	}

	private static void notifyClanMembers(L2PcInstance player)
	{
		L2Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		clan.getClanMember(player.getName()).setPlayerInstance(player);
		SystemMessage message = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
		message.addString(player.getName());
		clan.broadcastToOtherOnlineMembers(message, player);
		clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(player), player);
	}

	private static void notifySponsorOrApprentice(L2PcInstance player)
	{
		if (player.getSponsor() != 0)
		{
			L2PcInstance sponsor = (L2PcInstance) L2World.getInstance().findObject(player.getSponsor());
			if (sponsor != null)
			{
				SystemMessage message = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				message.addString(player.getName());
				sponsor.sendPacket(message);
			}
		}
		else if (player.getApprentice() != 0)
		{
			L2PcInstance apprentice = (L2PcInstance) L2World.getInstance().findObject(player.getApprentice());
			if (apprentice != null)
			{
				SystemMessage message = new SystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN);
				message.addString(player.getName());
				apprentice.sendPacket(message);
			}
		}
	}

	private static void setPledgeClass(L2PcInstance player)
	{
		int pledgeClass = 0;
		if (player.getClan() != null)
		{
			pledgeClass = player.getClan().getClanMember(player.getObjectId()).calculatePledgeClass(player);
		}
		if (player.isNoble() && (pledgeClass < 5))
		{
			pledgeClass = 5;
		}
		if (player.isHero())
		{
			pledgeClass = 8;
		}
		player.setPledgeClass(pledgeClass);
	}

	private static final class SingletonHolder
	{
		private static final PlayerWorldLifecycle INSTANCE = new PlayerWorldLifecycle();
	}
}
