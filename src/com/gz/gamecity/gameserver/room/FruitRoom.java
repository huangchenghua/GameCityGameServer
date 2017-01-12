package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.gameserver.table.FruitTable;

public class FruitRoom extends Room {

	private FruitTable table ;
	
	public FruitTable getTable() {
		return table;
	}

	public FruitRoom() {
		super(RoomType.Fruit);
		table = new FruitTable(this);
		addTable(table);
	}

	
}
