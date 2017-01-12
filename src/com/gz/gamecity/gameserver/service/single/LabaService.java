package com.gz.gamecity.gameserver.service.single;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.room.RoomManager;
import com.gz.gamecity.gameserver.room.RoomType;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.gameserver.table.LabaTable;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class LabaService implements LogicHandler {
	private static final Logger log =Logger.getLogger(LabaService.class);
	@Override
	public void handleMsg(BaseMsg msg) {
		Player player=PlayerManager.getPlayerFromMsg(msg);
		if(player==null){
			msg.closeChannel();
			log.info("数据异常，关闭连接");
			return;
		}
		ClientMsg cMsg=(ClientMsg)msg;
		int subCode = cMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch(subCode){
		case Protocols.C2g_laba_enter.subCode_value:
			handleEnterRoom(player,cMsg);
			break;
		case Protocols.C2g_laba_bet.subCode_value:
			handleBet(player,cMsg);
			break;
		case Protocols.C2g_laba_leave.subCode_value:
			handleLeaveRoom(player,cMsg);
			break;
		case Protocols.C2g_laba_guess.subCode_value:
			handleGuess(player,cMsg);
			break;
		case Protocols.C2g_laba_getpoint.subCode_value:
			getPoint(player,cMsg);
			break;
		}

	}

	private void getPoint(Player player,ClientMsg cMsg){
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Laba);
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
		LabaTable table = (LabaTable)t;
		
		table.getPoint(player,cMsg);
	}
	
	private void handleGuess(Player player, ClientMsg cMsg) {	
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Laba);
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
		LabaTable table = (LabaTable)t;
		
		table.handleGuess(player,cMsg);
	}

	private void handleLeaveRoom(Player player, ClientMsg cMsg) {
		Room room = RoomManager.getInstance().getRoom(RoomType.Laba);
		LabaTable table = (LabaTable)room.getTable(player.getTableId());
		if(table!=null){
			table.playerLeave(player.getUuid());
			room.playerLeave(player);
		}
		room.removeTable(table.getTableId());
	}


	/**
	 * 拉霸下注
	 * @param player
	 * @param cMsg
	 */
	private void handleBet(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		long bet = cMsg.getJson().getIntValue("bet");
		
		if(bet>player.getCoin()||bet<0){
			cMsg.closeChannel();
			log.info("数据异常，关闭连接");
			return;
		}
		
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Laba);
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
		LabaTable table = (LabaTable)t;
		table.putBet(player,bet);
		
		table.handleBet(player,cMsg);
	}


	private void handleEnterRoom(Player player,ClientMsg cMsg) {
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_laba_enter.subCode_value);
		Room room=RoomManager.getInstance().enterRoom(player, RoomType.Laba);
		if(room==null){
			cMsg.put(Protocols.ERRORCODE, "进入房间失败");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		LabaTable table=new LabaTable(room);
		boolean result=table.playerSitDown(player);
		if(result){
			room.addTable(table);
			table.player_star.put(player.getUuid(), 0);
		}else{
			cMsg.put(Protocols.ERRORCODE,"条件不满足");
		}
		
		
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.LABA;
	}

}
