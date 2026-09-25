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

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class QuestStateManager
{
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				cleanUp();
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
			}
			catch (Throwable t)
			{
			}
		}
	}
	
	private static QuestStateManager _instance;
	private List<QuestState> _questStates = new FastList<>();
	
	public QuestStateManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
	}
	
	/**
	 * Add QuestState for the specified player instance
	 * @param quest
	 * @param player
	 * @param state
	 * @param completed
	 */
	public void addQuestState(Quest quest, L2PcInstance player, State state, boolean completed)
	{
		QuestState qs = getQuestState(player);
		if (qs == null)
		{
			qs = new QuestState(quest, player, state, completed);
		}
	}
	
	/**
	 * Remove all QuestState for all player instance that does not exist
	 */
	public void cleanUp()
	{
		for (int i = getQuestStates().size() - 1; i >= 0; i--)
		{
			if (getQuestStates().get(i).getPlayer() == null)
			{
				removeQuestState(getQuestStates().get(i));
				getQuestStates().remove(i);
			}
		}
	}
	
	/**
	 * Remove QuestState instance
	 * @param qs
	 */
	private void removeQuestState(QuestState qs)
	{
		qs = null;
	}
	
	public static final QuestStateManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new QuestStateManager();
		}
		return _instance;
	}
	
	/**
	 * Return QuestState for specified player instance
	 * @param player
	 * @return
	 */
	public QuestState getQuestState(L2PcInstance player)
	{
		for (int i = 0; i < getQuestStates().size(); i++)
		{
			if ((getQuestStates().get(i).getPlayer() != null) && (getQuestStates().get(i).getPlayer().getObjectId() == player.getObjectId()))
			{
				return getQuestStates().get(i);
			}
			
		}
		
		return null;
	}
	
	/**
	 * Return all QuestState
	 * @return
	 */
	public List<QuestState> getQuestStates()
	{
		if (_questStates == null)
		{
			_questStates = new FastList<>();
		}
		return _questStates;
	}
}
