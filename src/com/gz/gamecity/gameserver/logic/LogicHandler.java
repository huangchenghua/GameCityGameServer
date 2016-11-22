package com.gz.gamecity.gameserver.logic;

import com.gz.websocket.msg.BaseMsg;

public interface LogicHandler {
	
	public void handleMsg(BaseMsg msg);
	
}
