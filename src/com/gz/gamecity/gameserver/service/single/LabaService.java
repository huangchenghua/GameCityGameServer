package com.gz.gamecity.gameserver.service.single;

import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;

public class LabaService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.LABA;
	}

}
