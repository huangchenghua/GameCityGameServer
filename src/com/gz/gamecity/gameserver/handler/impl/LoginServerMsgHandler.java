package com.gz.gamecity.gameserver.handler.impl;


import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.gameserver.GSMsgReceiver;
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
		System.out.println("与登录服的连接被断开");
		
	}

	@Override
	public void onConnect(Channel channel) {
		ProtocolMsg msg = new ProtocolMsg();
		JSONObject json =new JSONObject();
		json.put(Protocols.MAINCODE, Protocols.G2l_login.mainCode_value);
		json.put(Protocols.SUBCODE, Protocols.G2l_login.subCode_value);
		json.put(Protocols.G2l_login.SERVERID, Config.instance().getIValue(ConfigField.SERVER_ID));
		msg.setJson(json);
		msg.setChannel(channel);
		msg.sendSelf();
		//这里不能用sender来发送，因为这个时候验证没通过，sender的channel是空的
		//LoginMsgSender.getInstance().addMsg(msg);
	}

}
