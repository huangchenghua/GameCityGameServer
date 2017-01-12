package com.gz.gamecity.gameserver.service.niuniu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.room.NiuniuRoom;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.room.RoomManager;
import com.gz.gamecity.gameserver.room.RoomType;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.gameserver.table.NiuniuTable;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class NiuniuService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg msg) {
		Player player = null;
		if(!msg.isInner()){
			player=PlayerManager.getPlayerFromMsg(msg);
			if(player==null){
				msg.closeChannel();
				return;
			}
		}
		
		
		ClientMsg cMsg=(ClientMsg)msg;
		int subCode = cMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch(subCode){
		case Protocols.C2g_niuniu_enter.subCode_value:
			handleEnterRoom(player,cMsg);
			break;
		case Protocols.C2g_niuniu_choose_lvl.subCode_value:
			handleChooseLvl(player,cMsg);
			break;
		case Protocols.Inner_game_niuniu_start_bet.subCode_value:
			handleStartBet(cMsg);
			break;
		case Protocols.Inner_game_niuniu_checkout.subCode_value:
			handleCheckout(cMsg);
			break;
		case Protocols.C2g_niuniu_bet.subCode_value:
			handleBet(player,cMsg);
			break;
		case Protocols.C2g_niuniu_req_banker.subCode_value:
			handleReqBanker(player,cMsg);
			break;
		case Protocols.C2g_niuniu_quit_banker.subCode_value:
			handleQuitBanker(player,cMsg);
			break;
		case Protocols.C2g_niuniu_leave_table.subCode_value:
			handleReqLeaveTable(player,cMsg);
			break;
		case Protocols.C2g_niuniu_leave_room.subCode_value:
			handleReqLeaveRoom(player,cMsg);
			break;
		default:
			System.out.println("");	
				break;
		}

	}
	
	
	private void handleReqLeaveRoom(Player player, ClientMsg cMsg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_niuniu_leave_room.subCode_value);
		boolean result = true;
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			if(table.canLeave(player.getUuid())){
				table.playerLeave(player.getUuid());
			}else{
				cMsg.put(Protocols.ERRORCODE,"无法离开");
				result = false;
			}
		}
		if(result){
			room.playerLeave(player);
		}
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}


	private void handleReqLeaveTable(Player player, ClientMsg cMsg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_niuniu_leave_table.subCode_value);
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			if(table.canLeave(player.getUuid())){
				table.playerLeave(player.getUuid());
			}else{
				cMsg.put(Protocols.ERRORCODE,"无法离开");
			}
		}else{
			cMsg.put(Protocols.ERRORCODE,"未知的错误");
		}
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}


	private void handleQuitBanker(Player player, ClientMsg cMsg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			table.quitBanker(player,cMsg);
		}
	}


	private void handleReqBanker(Player player, ClientMsg cMsg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			table.reqBanker(player, cMsg);
		}
		
	}


	private void handleCheckout(ClientMsg cMsg) {
		String tableId=cMsg.getJson().getString(Protocols.Inner_game_niuniu_start_bet.TABLEID);
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			table.checkout();
		}
		
	}

	private void handleBet(Player player, ClientMsg cMsg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			table.putBet(player, cMsg);
		}
		
	}

	private void handleStartBet(ClientMsg cMsg) {
		String tableId=cMsg.getJson().getString(Protocols.Inner_game_niuniu_start_bet.TABLEID);
		Room room = RoomManager.getInstance().getRoom(RoomType.Niuniu);
		GameTable t = room.getTable(tableId);
		if(t!=null){
			NiuniuTable table = (NiuniuTable)t;
			table.gameStart();
		}
	}

	private void handleChooseLvl(Player player, ClientMsg cMsg) {
		JSONArray ja = AllTemplate.getNiuniu_level_jsonArray();
		//lvl表示玩家当前可以进哪个等级的桌子
		int lvl=0;
		JSONObject json_config=null;
		for(int i=0;i<ja.size();i++){
			json_config=ja.getJSONObject(i);
			if(player.getCoin()>=json_config.getLongValue("min") && player.getCoin()<=json_config.getLongValue("max")){
				lvl= json_config.getIntValue("level");
				break;
			}
		}
		if(lvl==0){
			cMsg.put(Protocols.ERRORCODE, "钱太少不够资格");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		if(lvl<cMsg.getJson().getIntValue(Protocols.C2g_niuniu_choose_lvl.LVL)){
			cMsg.put(Protocols.ERRORCODE, "钱太少不够资格");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		if(lvl>cMsg.getJson().getIntValue(Protocols.C2g_niuniu_choose_lvl.LVL)){
			cMsg.put(Protocols.ERRORCODE, "钱太多不够资格");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		
		// TODO 找桌子坐下
		NiuniuRoom room = (NiuniuRoom)RoomManager.getInstance().getRoom(RoomType.Niuniu);
		NiuniuTable table = room.findTable(lvl);
		boolean result=table.playerSitDown(player);
		if(result){
			room.addTable(table);
		}else{
			cMsg.put(Protocols.ERRORCODE,"条件不满足");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
	}

	private void handleEnterRoom(Player player,ClientMsg cMsg) {
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_enter.subCode_value);
		Room room=RoomManager.getInstance().enterRoom(player, RoomType.Niuniu);
		if(room==null){
			cMsg.put(Protocols.ERRORCODE, "进入房间失败");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.NIUNIU;
	}

}
