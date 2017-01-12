package com.gz.gamecity.gameserver.room;

import com.gz.gamecity.bean.Player;

public class LuckyWheelRoom extends Room{
		public LuckyWheelRoom(){
			super(RoomType.LuckyWheel);
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
