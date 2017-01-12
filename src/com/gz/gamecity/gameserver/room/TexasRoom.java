package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.gameserver.table.TexasTable;

public class TexasRoom extends Room {

	public TexasRoom() {
		super(RoomType.Texas);

	}
	
	@Override
	public boolean playerEnter(Player player){
		boolean bIsSucc = super.playerEnter(player);
		if (!bIsSucc) return false;
		
		
		
		return true;
	}
	
	@Override
	public boolean playerLeave(Player player) {
		super.playerLeave(player);
		
		return true;
	}
		
	public TexasTable findTableNotFullByLv(int nLv) {
		TexasTable table = null;
		for (GameTable t : tables.values()) {
			TexasTable tmp = (TexasTable)t;
			if (tmp.getLv() == nLv && !tmp.isFull()) {
				table = tmp;
				break;
			}
		}
		
		return table;
	}
}
