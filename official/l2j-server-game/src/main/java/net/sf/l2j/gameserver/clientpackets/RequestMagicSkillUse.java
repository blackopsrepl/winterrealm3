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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.player.PlayerCommandResult;
import net.sf.l2j.gameserver.model.actor.player.PlayerCommandService;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;

/**
 * @version $Revision: 1.7.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestMagicSkillUse extends L2GameClientPacket
{
	private static final String _C__2F_REQUESTMAGICSKILLUSE = "[C] 2F RequestMagicSkillUse";
	private static Logger _log = Logger.getLogger(RequestMagicSkillUse.class.getName());
	
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_magicId = readD(); // Identifier of the used skill
		_ctrlPressed = readD() != 0; // True if it's a ForceAttack : Ctrl pressed
		_shiftPressed = readC() != 0; // True if Shift pressed
	}
	
	@Override
	protected void runImpl()
	{
		// Get the current L2PcInstance of the player
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		PlayerCommandResult result = PlayerCommandService.getInstance().useSkill(activeChar, _magicId, _ctrlPressed, _shiftPressed);
		if (result == PlayerCommandResult.SKILL_NOT_FOUND)
		{
			_log.warning("No skill found for id " + _magicId + " at the player's learned level");
		}
		if (!result.isAccepted() && (result != PlayerCommandResult.RECALL_RESTRICTED))
		{
			activeChar.sendPacket(new ActionFailed());
		}
	}
	
	@Override
	public String getType()
	{
		return _C__2F_REQUESTMAGICSKILLUSE;
	}
}
