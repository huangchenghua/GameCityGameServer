package com.gz.gamecity.gameserver.service.single;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.room.RoomManager;
import com.gz.gamecity.gameserver.room.RoomType;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.gameserver.table.BlackATable;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class BlackAService implements LogicHandler{
	@Override
	public void handleMsg(BaseMsg msg) {
		Player player=PlayerManager.getPlayerFromMsg(msg);
		if(player==null){
			msg.closeChannel();
			return;
		}
		ClientMsg cMsg=(ClientMsg)msg;
		int subCode = cMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch(subCode){
		case Protocols.C2g_blackA_enter.subCode_value:
			handleEnterRoom(player,cMsg);
			break;
		case Protocols.C2g_blackA_bet.subCode_value:
			handleBet(player,cMsg);
			break;
		case Protocols.C2g_blackA_leave.subCode_value:
			handleLeaveRoom(player,cMsg);
			break;
		case Protocols.C2g_blackA_getbet.subCode_value:
			handleGetBet(player,cMsg);
			break;
		}

	}
	
	//收分
	private void handleGetBet(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		Room room=RoomManager.getInstance().getRoom(RoomType.BlackA);
		BlackATable table = (BlackATable)room.getTable(player.getTableId());
		cMsg.put("errorCode", "收分");
		table.player_star.put(player.getUuid(),0);
		PlayerDataService.getInstance().modifyCoin(player, table.player_reward.get(player.getUuid()),EventLogType.blackA_bet);
		table.player_reward.put(player.getUuid(),(long)0);
		table.player_bet.put(player.getUuid(),(long)0);
		cMsg.put("reward", table.player_reward.get(player.getUuid()));
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	private void handleLeaveRoom(Player player, ClientMsg cMsg) {
		Room room=RoomManager.getInstance().getRoom(RoomType.BlackA);
		BlackATable table = (BlackATable)room.getTable(player.getTableId());
		
		if(table!=null){
			table.playerLeave(player.getUuid());
			room.playerLeave(player);
		}
		room.removeTable(table.getTableId());
	}

	/**
	 * 大转盘下注
	 * @param player
	 * @param cMsg
	 */
	private void handleBet(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		RoomType type = RoomType.getRoomType(player.getRoomId());
   		Room room = RoomManager.getInstance().getRoom(type);
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.BlackA);
		if(t==null){
			player.getChannel().close();
			return;
		}
		BlackATable table = (BlackATable)room.getTable(player.getTableId());
		table.putBet(player, cMsg);
	}

	private void handleEnterRoom(Player player,ClientMsg cMsg) {
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_blackA_enter.subCode_value);
		Room room=RoomManager.getInstance().enterRoom(player, RoomType.BlackA);
		if(room==null){
			cMsg.put(Protocols.ERRORCODE, "进入房间失败");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		BlackATable table=  new BlackATable(room);
		//初始化星星数量
		table.putStar(player, 0);
		table.player_reward.put(player.getUuid(), (long)0);
		table.player_bet.put(player.getUuid(), (long)0);
		boolean result= table.playerSitDown(player);
		if(result){
			room.addTable(table);
		}else{
			cMsg.put(Protocols.ERRORCODE,"条件不满足");
		}
		
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.BLACKA;
	}
}
