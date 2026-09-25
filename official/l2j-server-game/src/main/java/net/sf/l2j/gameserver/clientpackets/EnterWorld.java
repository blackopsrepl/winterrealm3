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
package net.sf.l2j.gameserver.clientpackets;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.player.PlayerWorldLifecycle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.Die;
import net.sf.l2j.gameserver.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.serverpackets.FriendList;
import net.sf.l2j.gameserver.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.serverpackets.QuestList;
import net.sf.l2j.gameserver.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.serverpackets.SignsSky;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.FloodProtector;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev656 cbdddd
 * <p>
 * @version $Revision: 1.16.2.1.2.7 $ $Date: 2005/03/29 23:15:33 $
 */
public class EnterWorld extends L2GameClientPacket
{
	private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";
	private static Logger _log = Logger.getLogger(EnterWorld.class.getName());
	private static final PlayerWorldLifecycle PLAYER_WORLD_LIFECYCLE = PlayerWorldLifecycle.getInstance();
	
	public TaskPriority getPriority()
	{
		return TaskPriority.PR_URGENT;
	}
	
	@Override
	protected void readImpl()
	{
		// this is just a trigger packet. it has no content
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			_log.warning("EnterWorld failed! activeChar is null...");
			getClient().closeNow();
			return;
		}
		
		// Register in flood protector
		FloodProtector.getInstance().registerNewPlayer(activeChar.getObjectId());
		
		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			if (Config.DEBUG)
			{
				_log.warning("User already exist in OID map! User " + activeChar.getName() + " is character clone");
				// activeChar.closeNetConnection();
			}
		}
		
		if (activeChar.isGM())
		{
			if (Config.GM_STARTUP_INVULNERABLE && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_GODMODE)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invul"))))
			{
				activeChar.setIsInvul(true);
			}
			
			if (Config.GM_STARTUP_INVISIBLE && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_GODMODE)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invisible"))))
			{
				activeChar.getAppearance().setInvisible();
			}
			
			if (Config.GM_STARTUP_SILENCE && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_MENU)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_silence"))))
			{
				activeChar.setMessageRefusal(true);
			}
			
			if (Config.GM_STARTUP_AUTO_LIST && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_MENU)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_gmliston"))))
			{
				GmListTable.getInstance().addGm(activeChar, false);
			}
			else
			{
				GmListTable.getInstance().addGm(activeChar, true);
			}
			
			if (Config.GM_NAME_COLOR_ENABLED)
			{
				if (activeChar.getAccessLevel() >= 100)
				{
					activeChar.getAppearance().setNameColor(Config.ADMIN_NAME_COLOR);
				}
				else if (activeChar.getAccessLevel() >= 75)
				{
					activeChar.getAppearance().setNameColor(Config.GM_NAME_COLOR);
				}
			}
		}
		
		PLAYER_WORLD_LIFECYCLE.prepareSpawn(activeChar);
		PLAYER_WORLD_LIFECYCLE.spawn(activeChar);
		
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			sendPacket(new SignsSky());
		}
		
		// buff and status icons
		PLAYER_WORLD_LIFECYCLE.restoreEffects(activeChar);
		
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		
		// engage and notify Partner
		PLAYER_WORLD_LIFECYCLE.restoreRelationshipState(activeChar);
		PLAYER_WORLD_LIFECYCLE.finishPersistentRestore(activeChar);
		
		// Expand Skill
		ExStorageMaxCount esmc = new ExStorageMaxCount(activeChar);
		activeChar.sendPacket(esmc);
		
		activeChar.getMacroses().sendUpdate();
		
		sendPacket(new UserInfo(activeChar));
		
		sendPacket(new HennaInfo(activeChar));
		
		sendPacket(new FriendList(activeChar));
		
		sendPacket(new ItemList(activeChar, false));
		
		sendPacket(new ShortCutInit(activeChar));
		
		SystemMessage sm = new SystemMessage(SystemMessageId.WELCOME_TO_LINEAGE);
		sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString(getText("V2VsY29tZSB0byBhIEwySiBTZXJ2ZXIsIGZvdW5kZWQgYnkgTDJDaGVmLg=="));
		
		sendPacket(sm);
		sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString(getText("RGV2ZWxvcGVkIGJ5IHRoZSBMMkogRGV2IFRlYW0gYXQgbDJqc2VydmVyLmNvbS4="));
		
		sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString(getText("Q29weXJpZ2h0IDIwMDQtMjAwNw=="));
		sendPacket(sm);
		sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString(getText("V2VsY29tZSB0byA="));
		sm.addString(LoginServerThread.getInstance().getServerName());
		sendPacket(sm);
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);
		
		PLAYER_WORLD_LIFECYCLE.restoreQuestState(activeChar);
		activeChar.sendPacket(new QuestList());
		
		if (Config.SERVER_NEWS)
		{
			String serverNews = HtmCache.getInstance().getHtm("data/html/servnews.htm");
			if (serverNews != null)
			{
				sendPacket(new NpcHtmlMessage(1, serverNews));
			}
		}
		
		PetitionManager.getInstance().checkPetitionMessages(activeChar);
		
		// send user info again .. just like the real client
		// sendPacket(ui);
		
		if ((activeChar.getClanId() != 0) && (activeChar.getClan() != null))
		{
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		}
		
		if (activeChar.isAlikeDead())
		{
			// no broadcast needed since the player will already spawn dead to others
			sendPacket(new Die(activeChar));
		}
		
		PLAYER_WORLD_LIFECYCLE.prepareActivation(activeChar);
		PLAYER_WORLD_LIFECYCLE.activate(activeChar);
		PLAYER_WORLD_LIFECYCLE.enforceRestrictedLocations(activeChar);
		
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
		}
		
		if (activeChar.getClan() != null)
		{
			activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));
			
			PLAYER_WORLD_LIFECYCLE.refreshSiegeState(activeChar);
			// Add message at connexion if clanHall not paid.
			// Possibly this is custom...
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
				}
			}
		}
		
		PLAYER_WORLD_LIFECYCLE.enforceSiegeZone(activeChar);
		
		/*
		 * if(Config.GAMEGUARD_ENFORCE) - disabled by KenM will be reenabled later activeChar.sendPacket(new GameGuardQuery());
		 */
		
		PLAYER_WORLD_LIFECYCLE.completeEntry(activeChar);
	}
	
	/**
	 * @param string
	 * @return
	 */
	private String getText(String string)
	{
		try
		{
			String result = new String(Base64.decode(string), "UTF-8");
			return result;
		}
		catch (UnsupportedEncodingException e)
		{
			// huh, UTF-8 is not supported? :)
			return null;
		}
	}
	
	@Override
	public String getType()
	{
		return _C__03_ENTERWORLD;
	}
	
}
