package com.gz.gamecity.gameserver.service.common;

import java.util.concurrent.ConcurrentHashMap;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;

public class HallService implements LogicHandler {

	/**
	 * 所有通过验证登录的用户
	 */
	private ConcurrentHashMap<String,Player> hallPlayers=new ConcurrentHashMap<>();
	
	@Override
	public void handleMsg(BaseMsg msg) {
		

	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.HALL;
	}
	
	

}
