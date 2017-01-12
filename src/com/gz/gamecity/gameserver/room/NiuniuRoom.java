package com.gz.gamecity.gameserver.room;


import java.util.ArrayList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.gameserver.table.NiuniuTable;

public class NiuniuRoom extends Room {

	
	
	public NiuniuRoom() {
		super(RoomType.Niuniu);
	}

	public NiuniuTable findTable(int lvl){
		NiuniuTable table_find = null;
		for(GameTable t:tables.values()){
			NiuniuTable table=(NiuniuTable)t;
			if(table.getLvl() == lvl && !table.isFull()){
				if(table_find==null || table_find.getPlayerCount()<table.getPlayerCount()){
					table_find = table;
				}
			}
		}
		ArrayList<JSONObject> list_json=new ArrayList<>();
		//如果没有找到桌子，就新开一个桌子
		if(table_find == null){
			table_find = new NiuniuTable(this);
			table_find.setLvl(lvl);
			JSONArray ja = AllTemplate.getNiuniu_level_jsonArray();
			for(int i=0;i<ja.size();i++){
				JSONObject json_config=ja.getJSONObject(i);
				if(lvl == json_config.getIntValue("level")){
					list_json.add(json_config);
				}
			}
			table_find.setList_json(list_json);
		}
		return table_find;
	}
}
