package com.gz.gamecity.gameserver.room;

import java.util.concurrent.ConcurrentHashMap;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.table.GameTable;

public class RoomManager {
	
	private static RoomManager instance;
	
	private static ConcurrentHashMap<RoomType, Room> rooms;

	public static synchronized RoomManager getInstance() {
		if(instance == null)
			instance = new RoomManager();
		return instance;
	}
	
	private RoomManager(){
		rooms=new ConcurrentHashMap<>();
		addRoom(new LabaRoom());
		addRoom(new LuckyWheelRoom());
		addRoom(new BlackARoom());
		addRoom(new MahjongRoom());
		addRoom(new NiuniuRoom());
		addRoom(new TexasRoom());
  
		addRoom(new FruitRoom());

	}
	
	private void addRoom(Room room){
		rooms.put(room.getType(), room);
	}
	
	/**
	 * 进入房间，不成功返回null
	 * @param player
	 * @param roomType
	 * @return
	 */
	public Room enterRoom(Player player,RoomType roomType){
		if(player.getRoomId()!=0){ //不能同时进入两个房间
			return null;
		}
//		RoomType lastRoomType = RoomType.getRoomType(player.getRoomId());
//		if(lastRoomType!=null){
//			Room lastRoom=rooms.get(lastRoomType);
//			if(!lastRoom.playerLeave(player))
//				return null;
//		}
		Room room = rooms.get(roomType);
		room.playerEnter(player);
		return room;
	}
	
	public Room getRoom(RoomType roomType){
		return rooms.get(roomType);
	}
	public Room getRoom(int roomId){
		RoomType roomType = RoomType.getRoomType(roomId);
		if(roomType!=null)
			return rooms.get(roomType);
		return null;
	}
	
	public GameTable checkPlayerTable(Player player, RoomType roomType){
		//先判断房间是否有问题
		RoomType t = RoomType.getRoomType(player.getRoomId());
		if(t!=roomType)
			return null;
		//再判断桌子是否存在
		Room room = getInstance().getRoom(roomType);
		GameTable table = room.getTable(player.getTableId());
		return table;
	}
	
	public void playerReconnect(Player player){
		Room room = getRoom(player.getRoomId());
		if(room!=null && player.getTableId()!=null){
			if(player.getTableId()!=null){
				GameTable table = room.getTable(player.getTableId());
				if(table!=null && table.existPlayer(player))
					table.playerReconnect(player);
				else
					room.playerLeave(player);
			}
			else
				room.playerLeave(player);
		}
	}
	
	public void closeAllRoom(){
		for(Room room:rooms.values()){
			room.closeRoom();
		}
	}
}
