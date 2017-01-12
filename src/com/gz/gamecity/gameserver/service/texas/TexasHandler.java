package com.gz.gamecity.gameserver.service.texas;

import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.MessageContext;

import com.alibaba.fastjson.*;

import org.apache.log4j.Logger;

import com.gz.gamecity.protocol.Protocols;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.room.RoomManager;
import com.gz.gamecity.gameserver.room.RoomType;
import com.gz.gamecity.gameserver.room.TexasRoom;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.gameserver.table.NiuniuTable;
import com.gz.gamecity.gameserver.table.TexasTable;

import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;


public class TexasHandler implements LogicHandler {

	public static final Logger log = Logger.getLogger(TexasHandler.class);

	@Override
	public void handleMsg(BaseMsg msg) {
		// TODO Auto-generated method stub
		Player player = null;
		if (!msg.isInner()) {
			player = PlayerManager.getPlayerFromMsg(msg);
			if (player == null) {
				log.error("player not find, close websocket");
				msg.closeChannel();
				return;
			}
		}
		
		ClientMsg clientMsg = (ClientMsg)msg;
		
		int subCode = clientMsg.getJson().getIntValue(Protocols.SUBCODE);
		
		String strUuid = player == null ? "null" : player.getUuid();
		log.debug("handle texas [uuid=" + strUuid + " subCode=" + subCode + "]");
			
		switch (subCode) {
		case Protocols.C2g_texas_enter_room.subCode_value :
			HandleEnterRoom(player, clientMsg);
			break;
			
		case Protocols.C2g_texas_choose_lv.subCode_value :
			HandleChooseLv(player, clientMsg);
			break;
			
		case Protocols.C2g_texas_bet.subCode_value :
			HandleBet(player, clientMsg);
			break;
			
		case Protocols.C2g_texas_leave_room.subCode_value :
			HandleLeaveRoom(player, clientMsg);
			break;
			
		case Protocols.Inner_game_texas_start.subCode_value :
			HandleGameStart(clientMsg);
			break;
			
		case Protocols.Inner_game_texas_player_action.subCode_value:
			HandlePlayerAction(clientMsg);
			break;
			
		default :
			log.error("not find subCode: " + subCode);

		}
	}

	@Override
	public int getMainCode() {
		// TODO Auto-generated method stub
		return Protocols.MainCode.TEXAS;
	}
	
	private void HandleLeaveTable(Player player,  ClientMsg clientMsg) {
		//String strTableId = clientMsg.getJson().getString(Protocols.Inner_game_texas_player_action.TABLEID);
		
		Room room = RoomManager.getInstance().getRoom(RoomType.Texas);
		room.playerLeave(player);
	
		GameTable t = room.getTable(player.getTableId());

		if (t != null) {
			TexasTable texasTable = (TexasTable)t;
			texasTable.removePlayer(player.getUuid());
		}
		
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_levae_table.subCode_value);
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
	
		log.info("leave texas table [uuid=" + player.getUuid() + " tableId=" + player.getTableId() + "]");
	}
	
	private void HandlePlayerAction(ClientMsg clientMsg) {
		String strTableId = clientMsg.getJson().getString(Protocols.Inner_game_texas_player_action.TABLEID);
		int nSeatIndex = clientMsg.getJson().getIntValue(Protocols.Inner_game_texas_player_action.SEAT_INDEX);
		int nRound = clientMsg.getJson().getIntValue(Protocols.Inner_game_texas_player_action.ROUND);
		log.debug("inner texas player action[table_id=" + strTableId + " seat_index=" + nSeatIndex + " round=" + nRound + "]");
		
		Room room = RoomManager.getInstance().getRoom(RoomType.Texas);
		TexasTable texasTable = (TexasTable) room.getTable(strTableId);
		if (texasTable == null) {
			log.error("not find texas table [table_id=" + strTableId + "]");
			return ;
		}
		texasTable.playerDefaultAction();
	}
	
	private void HandleGameStart(ClientMsg clientMsg) {
		String strTableId = clientMsg.getJson().getString(Protocols.Inner_game_texas_start.TABLEID);
		Room room = RoomManager.getInstance().getRoom(RoomType.Texas);
		TexasTable texasTable = (TexasTable) room.getTable(strTableId);
		if (texasTable == null) {
			log.error("not find texas table [table_id=" + strTableId + "]");
			return ;
		}
		log.debug("handle inner game texas start [tableId=" + strTableId + "]");
		texasTable.tryRunRound();
	}
	
	private void HandleLeaveRoom(Player player, ClientMsg clientMsg) {
		
		Room room = RoomManager.getInstance().getRoom(RoomType.Texas);
		room.playerLeave(player);
	
		GameTable t = room.getTable(player.getTableId());

		if (t != null) {
			TexasTable texasTable = (TexasTable)t;
			texasTable.removePlayer(player.getUuid());
		}
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_leave_room.subCode_value);
		clientMsg.put(Protocols.G2c_texas_leave_room.UUID, player.getUuid());
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
		log.info("leave texas room [uuid=" + player.getUuid() + "]");
	}
	
	private void HandleBet(Player player, ClientMsg clientMsg) {
		int nActionType = clientMsg.getJson().getIntValue("action_type");
		long nBet = clientMsg.getJson().getLongValue("bet");
		TexasRoom room = (TexasRoom)RoomManager.getInstance().getRoom(RoomType.Texas);
		TexasTable table = (TexasTable)room.getTable(player.getTableId());
		
		table.playerAction(player, nActionType, nBet);
	}
	
	private void HandleEnterRoom(Player player, ClientMsg clientMsg) {
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_enter_room.subCode_value);
		Room room = RoomManager.getInstance().enterRoom(player, RoomType.Texas);
		if ( room == null )
		{
			PlayerMsgSender.getInstance().addMsg(clientMsg);
			
			clientMsg.put(Protocols.ERRORCODE, "进入房间失败");
			PlayerMsgSender.getInstance().addMsg(clientMsg);
			return ;
		}
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
		log.info("enter texas room [uuid=" + player.getUuid() + "]");
	}
	
	private void HandleChooseLv(Player player, ClientMsg clientMsg) {		
		int nChooseLv = clientMsg.getJson().getIntValue(Protocols.C2g_texas_choose_lv.LV);
		
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_choose_lv.subCode_value);
		
		JSONObject json_config=null;
		JSONArray ja = AllTemplate.getTexas_level_jsonArray();
		for( int i = 0; i < ja.size(); ++i) {
			JSONObject tmp = ja.getJSONObject(i);
			if (tmp.getIntValue("lv") == nChooseLv) {
				json_config = tmp;
				break;
			}
		}
		if (json_config == null) {
			clientMsg.put(Protocols.ERRORCODE, "找不到房间(lv=" + nChooseLv + ")");
			PlayerMsgSender.getInstance().addMsg(clientMsg);
			return;
		}
		
		if (player.getCoin() < json_config.getLongValue("banker_condition")) {
			clientMsg.put(Protocols.ERRORCODE, "钱太少不够资格");
			PlayerMsgSender.getInstance().addMsg(clientMsg);
			return ;
		}
		
		if (player.getRoomId() == 0) {
			log.debug("player not in room, try into texas room[uuid=" + player.getUuid() + "]");
			Room room = RoomManager.getInstance().enterRoom(player, RoomType.Texas);
			if ( room == null )
			{
				PlayerMsgSender.getInstance().addMsg(clientMsg);
				clientMsg.put(Protocols.ERRORCODE, "进入房间失败");
				PlayerMsgSender.getInstance().addMsg(clientMsg);
				return ;
			}
		}
		
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
		TexasRoom room = (TexasRoom)RoomManager.getInstance().getRoom(RoomType.Texas);
		TexasTable table = room.findTableNotFullByLv(nChooseLv);
		
		if (table == null) {
			table = new TexasTable(room);
			table.setJSONData(json_config);
		}
		
		if (!table.playerSitDown(player)) {
			clientMsg.put(Protocols.ERRORCODE,"条件不满足");
			PlayerMsgSender.getInstance().addMsg(clientMsg);
			return;
		}
		
		room.addTable(table);

		log.info("choose texas table[uuid=" + player.getUuid() + " lv=" + nChooseLv + "]");
	}
}
