package com.gz.gamecity.gameserver.room;

public class RoomManager {
	
	private static RoomManager instance;

	public static synchronized RoomManager getInstance() {
		if(instance == null)
			instance = new RoomManager();
		return instance;
	}
	
	private RoomManager(){
		
	}
	
	
	
}
