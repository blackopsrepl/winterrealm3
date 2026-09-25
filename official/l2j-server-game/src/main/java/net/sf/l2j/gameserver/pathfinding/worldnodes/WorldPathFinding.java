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
package net.sf.l2j.gameserver.pathfinding.worldnodes;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;
import net.sf.l2j.gameserver.pathfinding.Node;
import net.sf.l2j.gameserver.pathfinding.PathFinding;

/**
 * @author -Nemesiss-
 */
public class WorldPathFinding extends PathFinding
{
	private static WorldPathFinding _instance;
	@SuppressWarnings("unused")
	private static Map<Short, ByteBuffer> _pathNodes = new FastMap<>();
	private static Map<Short, IntBuffer> _pathNodesIndex = new FastMap<>();
	
	public static WorldPathFinding getInstance()
	{
		if (_instance == null)
		{
			_instance = new WorldPathFinding();
		}
		return _instance;
	}
	
	@Override
	public boolean pathNodesExist(short regionoffset)
	{
		return _pathNodesIndex.containsKey(regionoffset);
	}
	
	// TODO! [Nemesiss]
	@Override
	public List<AbstractNodeLoc> findPath(int gx, int gy, short z, int gtx, int gtz, short tz)
	{
		return null;
	}
	
	@Override
	public Node[] readNeighbors(short node_x, short node_y, int idx)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	private WorldPathFinding()
	{
		// TODO! {Nemesiss] Load PathNodes.
	}
}
