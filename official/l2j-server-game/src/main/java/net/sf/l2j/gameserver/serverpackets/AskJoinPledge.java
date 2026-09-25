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
package net.sf.l2j.gameserver.serverpackets;

/**
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class AskJoinPledge extends L2GameServerPacket
{
	private static final String _S__44_ASKJOINPLEDGE = "[S] 32 AskJoinPledge";
	
	private final int _requestorObjId;
	private final String _pledgeName;
	
	public AskJoinPledge(int requestorObjId, String pledgeName)
	{
		_requestorObjId = requestorObjId;
		_pledgeName = pledgeName;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x32);
		writeD(_requestorObjId);
		writeS(_pledgeName);
	}
	
	@Override
	public String getType()
	{
		return _S__44_ASKJOINPLEDGE;
	}
	
}
