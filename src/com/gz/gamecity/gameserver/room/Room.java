package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;
import com.gz.websocket.msg.ClientMsg;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Room {
	
	protected ConcurrentHashMap<String,Player> roomPlayers=new ConcurrentHashMap<>();
	
	public abstract void playerEnter(Player player);
	
	public abstract void playerLeave(Player player);
	
	public void sendRoomMsg(ClientMsg msg){
		
	}
	
	public abstract int getRoomId();
	
}
