package com.gz.gamecity.gameserver.handler.impl;


import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.protocol.ProtocolsField;
import com.gz.util.Config;
import com.gz.websocket.msg.ProtocolMsg;
import com.gz.websocket.protocol.client.ProtocolClientMsgHandler;

import io.netty.channel.Channel;

public class LoginServerMsgHandler implements ProtocolClientMsgHandler{

	@Override
	public void onMsgReceived(ProtocolMsg msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionClosed(Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnect(Channel channel) {
		ProtocolMsg msg = new ProtocolMsg();
		JSONObject json =new JSONObject();
		json.put(ProtocolsField.MAINCODE, ProtocolsField.G2l_login.mainCode_value);
		json.put(ProtocolsField.SUBCODE, ProtocolsField.G2l_login.subCode_value);
		json.put(ProtocolsField.G2l_login.SERVERID, Config.instance().getIValue(ConfigField.SERVER_ID));

		String body = json.toJSONString();
		msg.setContent(body);
		channel.writeAndFlush(msg);
	}

}
