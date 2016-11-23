package com.gz.gamecity.gameserver.service;

import com.gz.gamecity.gameserver.logic.LogicHandler;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class LoginServerService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg msg) {
		ProtocolMsg pMsg=(ProtocolMsg)msg;
		
	}

}
