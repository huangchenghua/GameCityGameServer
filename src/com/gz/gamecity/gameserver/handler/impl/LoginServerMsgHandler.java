package com.gz.gamecity.gameserver.handler.impl;


import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.gamecity.gameserver.LSConnecter;
import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.Config;
import com.gz.websocket.msg.ProtocolMsg;
import com.gz.websocket.protocol.client.ProtocolClientMsgHandler;

import io.netty.channel.Channel;

public class LoginServerMsgHandler implements ProtocolClientMsgHandler{

	@Override
	public void onMsgReceived(ProtocolMsg msg) {
		msg.parse();
		GSMsgReceiver.getInstance().addMsg(msg);
	}

	@Override
	public void onSessionClosed(Channel channel) {
		System.out.println("与登录服的连接被断开====================================");
//		LSConnecter.getInstance().connectLoginServer();
	}

	@Override
	public void onConnect(Channel channel) {
		ProtocolMsg msg = new ProtocolMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2l_login.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_login.subCode_value);
		msg.put(Protocols.G2l_login.SERVERID, Config.instance().getIValue(ConfigField.SERVER_ID));
		msg.setChannel(channel);
		msg.sendSelf();
		//这里不能用sender来发送，因为这个时候验证没通过，sender的channel是空的
		//LoginMsgSender.getInstance().addMsg(msg);
	}

	@Override
	public void onExceptionCaught(Channel channel, Throwable cause) {
		System.out.println("网络异常"+cause.toString());
		try {
			channel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		LSConnecter.getInstance().connectLoginServer();
	}
	
}
