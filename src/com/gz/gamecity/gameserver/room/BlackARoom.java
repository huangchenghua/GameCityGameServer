package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;

public class BlackARoom extends Room{
	public BlackARoom(){
		super(RoomType.BlackA);
	}
	

	@Override
	public boolean playerEnter(Player player) {
		super.playerEnter(player);
		
		return true;
	}

	@Override
	public boolean playerLeave(Player player) {
		super.playerLeave(player);
		
		return true;
	}
}
