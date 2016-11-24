package com.gz.gamecity.gameserver.service;

import com.gz.gamecity.gameserver.GameServiceMain;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.logic.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class LoginServerService implements LogicHandler {

	
	@Override
	public void handleMsg(BaseMsg msg) {
		ProtocolMsg pMsg=(ProtocolMsg)msg;
		String opt=pMsg.getJson().getString(Protocols.L2g_login.OPT);
		if(opt!=null && opt.equals(Protocols.ProtocolConst.L2G_LOGIN_OPT_SUC)){
			LoginMsgSender.getInstance().setChannel(msg.getChannel());
			GameServiceMain.getInstance().setConnected(true);
		}
	}

}
