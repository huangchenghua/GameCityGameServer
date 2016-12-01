package com.gz.gamecity.gameserver.service.common;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;

public class PlayerDataService implements LogicHandler {

	private static PlayerDataService instance;
	
	
	
	public static synchronized PlayerDataService getInstance() {
		if(instance==null)
			instance=new PlayerDataService();
		return instance;
	}

	private PlayerDataService(){
		
	}
	
	@Override
	public void handleMsg(BaseMsg msg) {
		

	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYER_DATA_GAME;
	}

	public void modifyCoin(Player player,long change){
		long result = player.getCoin()+change;
		if(result<0){
			player.setCoin(0);
		}else{
			player.setCoin(result);
		}
		
	}
	
}
