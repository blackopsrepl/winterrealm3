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
package net.sf.l2j.gameserver.model.quest;

import net.sf.l2j.gameserver.instancemanager.QuestManager;

/**
 * Functions in this class are used in python files.
 * @author Luis Arias
 */
public class State
{
	/** Name of the quest */
	private final String _questName;
	private final String _name;
	
	/**
	 * Constructor for the state of the quest.
	 * @param name : String pointing out the name of the quest
	 * @param quest : Quest
	 */
	public State(String name, Quest quest)
	{
		_name = name;
		_questName = quest.getName();
		quest.addState(this);
	}
	
	/**
	 * Add drop for the quest at this state of the quest
	 * @param npcId : int designating the ID of the NPC
	 * @param itemId : int designating the ID of the item dropped
	 * @param chance : int designating the chance the item dropped DEPRECATING THIS...only the itemId is really needed, and even that is only here for backwards compatibility
	 */
	public void addQuestDrop(int npcId, int itemId, int chance)
	{
		QuestManager.getInstance().getQuest(_questName).registerItem(itemId);
	}
	
	/**
	 * Return name of the quest
	 * @return String
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Return name of the quest
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name;
	}
}
