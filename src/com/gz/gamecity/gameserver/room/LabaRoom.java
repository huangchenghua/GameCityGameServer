package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;

public class LabaRoom extends Room {

	
	public LabaRoom(){
		super(RoomType.Laba);
	}
	


	@Override
	public boolean playerLeave(Player player) {
		super.playerLeave(player);
		
		return true;
	}



}
