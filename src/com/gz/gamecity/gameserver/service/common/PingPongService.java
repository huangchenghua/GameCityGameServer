package com.gz.gamecity.gameserver.service.common;

import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class PingPongService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg bMsg) {
		ClientMsg msg=(ClientMsg) bMsg;
		msg.put(Protocols.SUBCODE, Protocols.G2c_heart_pong.subCode_value);
		PlayerMsgSender.getInstance().addMsg(msg);
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.HEART;
	}

}
