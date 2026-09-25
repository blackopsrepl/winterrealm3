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
package net.sf.l2j.gameserver.pathfinding;

/**
 * @author -Nemesiss-
 */
public class Node
{
	private final AbstractNodeLoc _loc;
	private final int _neighborsIdx;
	private Node[] _neighbors;
	private Node _parent;
	private short _cost;
	
	public Node(AbstractNodeLoc Loc, int Neighbors_idx)
	{
		_loc = Loc;
		_neighborsIdx = Neighbors_idx;
	}
	
	public void setParent(Node p)
	{
		_parent = p;
	}
	
	public void setCost(int cost)
	{
		_cost = (short) cost;
	}
	
	public void attacheNeighbors()
	{
		if (_loc == null)
		{
			_neighbors = null;
		}
		else
		{
			_neighbors = PathFinding.getInstance().readNeighbors(_loc.getNodeX(), _loc.getNodeY(), _neighborsIdx);
		}
	}
	
	public Node[] getNeighbors()
	{
		return _neighbors;
	}
	
	public Node getParent()
	{
		return _parent;
	}
	
	public AbstractNodeLoc getLoc()
	{
		return _loc;
	}
	
	public short getCost()
	{
		return _cost;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 1117;
		int result = 1;
		result = (prime * result) + _loc.getX();
		result = (prime * result) + _loc.getY();
		result = (prime * result) + _loc.getZ();
		return result;
	}
	
	@Override
	public boolean equals(Object arg0)
	{
		if (!(arg0 instanceof Node))
		{
			return false;
		}
		Node n = (Node) arg0;
		// Check if x,y,z are the same
		return (_loc.getX() == n.getLoc().getX()) && (_loc.getY() == n.getLoc().getY()) && (_loc.getZ() == n.getLoc().getZ());
	}
}
