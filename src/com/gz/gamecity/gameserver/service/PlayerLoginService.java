package com.gz.gamecity.gameserver.service;

import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.logic.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

import io.netty.util.AttributeKey;

public class PlayerLoginService implements LogicHandler {
	
	private static final AttributeKey<Player> NETTY_CHANNEL_KEY = AttributeKey.valueOf("player");
	
	private static HashMap<String, Player> map_loginPlayer=new HashMap<String, Player>();
	
	private static PlayerLoginService instance;
	
	
	
	public static synchronized PlayerLoginService getInstance() {
		if(instance ==null)
			instance = new PlayerLoginService();
		return instance;
	}
	
	private PlayerLoginService() {

	}
	@Override
	public void handleMsg(BaseMsg msg) {
		ClientMsg cMsg=(ClientMsg)msg;
		int subCode = cMsg.getJson().getIntValue("subCode");
		switch (subCode) {
		case Protocols.C2g_login.subCode_value:
			handlePlayerLogin(cMsg);
			break;
		default:
			break;
		}
	}
	
	private void handlePlayerLogin(ClientMsg cMsg) {
		String uuid=cMsg.getJson().getString(Protocols.C2g_login.UUID);
		String gameToken=cMsg.getJson().getString(Protocols.C2g_login.GAMETOKEN);
		if(uuid==null||uuid.equals("")||gameToken==null||gameToken.equals(""))
		{
			// TODO 验证不通过
			System.out.println("参数异常");
		}
		
		Player player=new Player();
		player.setChannel(cMsg.getChannel());
		player.setUuid(uuid);
		player.setGameToken(gameToken);
		map_loginPlayer.put(uuid, player);
		
		
		JSONObject json=new JSONObject();
		json.put(Protocols.MAINCODE, Protocols.G2l_playerVerify.mainCode_value);
		json.put(Protocols.SUBCODE, Protocols.G2l_playerVerify.subCode_value);
		ProtocolMsg msg = new ProtocolMsg();
		msg.setJson(json);
		LoginMsgSender.getInstance().addMsg(msg);
		
	}

	public Player getOnlinePlayer(String uuid){
		return map_loginPlayer.get(uuid);
	}
	
	public void playerLogout(String uuid){
		Player player=map_loginPlayer.remove(uuid);
		// TODO 通知登录服务器
	}
}
