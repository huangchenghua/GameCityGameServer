package com.gz.gamecity.gameserver.service;

import com.gz.websocket.msg.BaseMsg;

public interface LogicHandler {
	
	public void handleMsg(BaseMsg msg);
	
	public int getMainCode();
}
