package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;

public class MahjongRoom extends Room {

	
	public MahjongRoom(){
		super(RoomType.Mahjong);
	}
	


	@Override
	public boolean playerLeave(Player player) {
		super.playerLeave(player);
		
		return true;
	}



}
