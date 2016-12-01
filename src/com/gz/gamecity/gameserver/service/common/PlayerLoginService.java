package com.gz.gamecity.gameserver.service.common;


import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class PlayerLoginService implements LogicHandler {
	
	
//	private static HashMap<String, Player> map_loginPlayer=new HashMap<String, Player>();
	
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
		int subCode = cMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.C2g_login.subCode_value:
			handlePlayerLogin(cMsg);
			break;
		case Protocols.Inner_game_player_logout.subCode_value:
			handlePlayerLogout(cMsg);
			break;
		default:
			break;
		}
	}
	
	private void handlePlayerLogout(ClientMsg cMsg) {
		String uuid = cMsg.getJson().getString(Protocols.Inner_game_player_logout.UUID);
		PlayerManager.getInstance().playerOffline(uuid);
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
		PlayerManager.getInstance().playerOnline(player);
		Attribute<Player> att= cMsg.getChannel().attr(Player.NETTY_CHANNEL_KEY);
		att.setIfAbsent(player);
//		map_loginPlayer.put(uuid, player);
		
		
		JSONObject json=new JSONObject();
		json.put(Protocols.MAINCODE, Protocols.G2l_playerVerify.mainCode_value);
		json.put(Protocols.SUBCODE, Protocols.G2l_playerVerify.subCode_value);
		json.put(Protocols.G2l_playerVerify.UUID, uuid);
		json.put(Protocols.G2l_playerVerify.GAMETOKEN, gameToken);
		ProtocolMsg msg = new ProtocolMsg();
		msg.setJson(json);
		LoginMsgSender.getInstance().addMsg(msg);
		
	}


	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYER_GAME_LOGIN;
	}
}
