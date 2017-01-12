package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.websocket.msg.ClientMsg;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Room {
	
	private RoomType type;

	protected ConcurrentHashMap<String,Player> roomPlayers=new ConcurrentHashMap<>();
	
	protected ConcurrentHashMap<String, GameTable> tables =new ConcurrentHashMap<>();
	
	protected Room(RoomType type){
		this.type = type;
	}
	
	public RoomType getType() {
		return type;
	}

	public boolean playerEnter(Player player){
		roomPlayers.put(player.getUuid(), player);
		player.setRoomId(this.type.getRoomId());
		return true;
	}
	
	public boolean playerLeave(Player player){
		roomPlayers.remove(player.getUuid());
		player.setRoomId(0);
		return true;
	}
	
	public void sendRoomMsg(ClientMsg msg){
		
	}
	
	public GameTable getTable(String tableId){
		if(tableId==null)
			return null;
		return tables.get(tableId);
	}
	
	public void removeTable(String tableId){
		tables.remove(tableId);
	}
	
	public void addTable(GameTable table){
		tables.put(table.getTableId(), table);
	}
}
