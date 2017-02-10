package com.gz.gamecity.gameserver.service.common;


import java.util.Random;
import java.util.UUID;

import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.room.RoomManager;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.gameserver.table.GameTable;
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
		case Protocols.C2g_test_login.subCode_value:
			handleTestPlayerLogin(cMsg);
			break;
		case Protocols.C2g_init_suc.subCode_value:
			handleClientInitSuc(cMsg);
			break; 
		default:
			break;
		}
	}
	
	private void handleClientInitSuc(ClientMsg cMsg) {
		Player player=PlayerManager.getPlayerFromMsg(cMsg);
		if(player==null){
			cMsg.getChannel().close();
			return;
		}
		RoomManager.getInstance().playerReconnect(player);
	}

	private void handleTestPlayerLogin(ClientMsg cMsg) {
		long coin=cMsg.getJson().getLongValue(Protocols.C2g_test_login.COIN);
		Player player=new Player();
		player.setChannel(cMsg.getChannel());
		player.setUuid(UUID.randomUUID().toString());
		player.setGameToken(UUID.randomUUID().toString());
		player.setName("测试玩家"+new Random().nextInt(1000));
		player.setCoin(coin);
		player.setOnline(true);
		PlayerManager.getInstance().playerOnline(player);
		PlayerManager.bindPlayer(cMsg, player);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_login.subCode_value);
		cMsg.put(Protocols.G2c_login.NAME, player.getName());
		cMsg.put(Protocols.G2c_login.COIN, player.getCoin());
		cMsg.put(Protocols.G2c_login.TIMESTAMP, System.currentTimeMillis());
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	private void handlePlayerLogout(ClientMsg cMsg) {
		String uuid = cMsg.getJson().getString(Protocols.Inner_game_player_logout.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player==null)
			return;
		Room room = RoomManager.getInstance().getRoom(player.getRoomId());
		if(room!=null){
			GameTable table = room.getTable(player.getTableId());
			if(table!=null){
				if(!table.canLeave(player.getUuid())){
					//如果玩家在的游戏或者牌局还未结算，就不能走下线流程
					return;
				}else{
					//如果可以离开桌子，就执行离开桌子的流程
					table.playerLeave(player.getUuid());
				}
			}
			room.playerLeave(player);
		}
		PlayerManager.getInstance().playerOffline(uuid);
	}

	private void handlePlayerLogin(ClientMsg cMsg) {
		String uuid=cMsg.getJson().getString(Protocols.C2g_login.UUID);
		String gameToken=cMsg.getJson().getString(Protocols.C2g_login.GAMETOKEN);
		if(uuid==null||uuid.equals("")||gameToken==null||gameToken.equals(""))
		{
			// TODO 验证不通过
			System.out.println("参数异常");
			return;
		}
		
		Player player=new Player();
		player.setChannel(cMsg.getChannel());
		player.setUuid(uuid);
		player.setGameToken(gameToken);
		player.setOnline(true);
		PlayerManager.getInstance().playerLogin(player);
		PlayerManager.bindPlayer(cMsg, player);
//		Attribute<Player> att= cMsg.getChannel().attr(Player.NETTY_CHANNEL_KEY);
//		att.setIfAbsent(player);
//		map_loginPlayer.put(uuid, player);
		

		ProtocolMsg msg = new ProtocolMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2l_playerVerify.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_playerVerify.subCode_value);
		msg.put(Protocols.G2l_playerVerify.UUID, uuid);
		msg.put(Protocols.G2l_playerVerify.GAMETOKEN, gameToken);
		LoginMsgSender.getInstance().addMsg(msg);
		
	}


	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYER_GAME_LOGIN;
	}
}
