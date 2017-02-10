package com.gz.gamecity.gameserver.service.single;

import java.lang.reflect.Array;
import java.util.ArrayList;

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
import com.gz.gamecity.gameserver.table.MahjongTable;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class MahjongService implements LogicHandler{
	private static final Logger log =Logger.getLogger(MahjongService.class);
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
		case Protocols.C2g_mahjong_enter.subCode_value:
			handleEnterRoom(player,cMsg);
			break;
		case Protocols.C2g_mahjong_bet.subCode_value:
			handleBet(player,cMsg);
			break;
		case Protocols.C2g_mahjong_getview.subCode_value:
			getView(player,cMsg);
			break;
		case Protocols.C2g_mahjong_getpoint.subCode_value:
			getPoint(player,cMsg);
			break;
		case Protocols.C2g_mahjong_leave.subCode_value:
			handleLeaveRoom(player,cMsg);
			break;
		case Protocols.C2g_mahjong_start.subCode_value:
			startGame(player,cMsg);
			break;
		}
	}

	private void startGame(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Mahjong);
		MahjongTable table = (MahjongTable)t;
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
			
		table.startGame(player,cMsg);
		
	}

	private void getPoint(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Mahjong);
		MahjongTable table = (MahjongTable)t;
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
			
		table.getPoint(player,cMsg);
		
	}

	private void getView(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Mahjong);
		MahjongTable table = (MahjongTable)t;
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
			
		table.getView(player,cMsg);
		
	}

	private void handleLeaveRoom(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		Room room = RoomManager.getInstance().getRoom(RoomType.Mahjong);
		MahjongTable table = (MahjongTable)room.getTable(player.getTableId());
		if(table!=null){
			table.playerLeave(player.getUuid());
			room.playerLeave(player);
		}
		room.removeTable(table.getTableId());
	}

	private void handleBet(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		long bet = cMsg.getJson().getIntValue(Protocols.C2g_mahjong_bet.BET);
		
		if(bet>player.getCoin()||bet<0){
			cMsg.closeChannel();
			log.info("数据异常，关闭连接");
			return;
		}
		
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.Mahjong);
		if(t==null){
			player.getChannel().close();
			log.info("数据异常，关闭连接");
			return;
		}
		MahjongTable table = (MahjongTable)t;
		table.putBet(player,bet);
		
		table.player_probability.put(player.getUuid(),cMsg.getJson().getIntValue(Protocols.C2g_mahjong_bet.PROBABILITY));
		
		table.lock.put(player.getUuid(), cMsg.getJson().getBoolean(Protocols.C2g_mahjong_bet.LOCK));
		
		table.handleRandom(player,cMsg);	
	}

	private void handleEnterRoom(Player player, ClientMsg cMsg) {
		// TODO Auto-generated method stub
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_mahjong_enter.subCode_value);
		Room room=RoomManager.getInstance().enterRoom(player, RoomType.Mahjong);
		MahjongTable table=new MahjongTable(room);
		boolean result=table.playerSitDown(player);
		if(result){
			room.addTable(table);
		}else{
			cMsg.put(Protocols.ERRORCODE,AllTemplate.getGameString("str18"));
		}
		
		table.enterRoom(player,cMsg);
	}

	@Override
	public int getMainCode() {
		// TODO Auto-generated method stub
		return Protocols.MainCode.MAHJONG;
	}

}
