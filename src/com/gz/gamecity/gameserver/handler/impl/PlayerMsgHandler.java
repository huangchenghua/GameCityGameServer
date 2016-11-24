package com.gz.gamecity.gameserver.handler.impl;

import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.server.ServerMsgHandler;

import io.netty.channel.Channel;

public class PlayerMsgHandler implements ServerMsgHandler{

	@Override
	public void onMsgReceived(BaseMsg msg) {
		ClientMsg cMsg=new ClientMsg(msg);
		cMsg.parse();
		GSMsgReceiver.getInstance().addMsg(cMsg);
		
	}

	@Override
	public void onSessionClosed(Channel channel) {
		// TODO Auto-generated method stub
		
	}

}
