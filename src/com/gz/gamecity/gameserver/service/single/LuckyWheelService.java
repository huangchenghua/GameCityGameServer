package com.gz.gamecity.gameserver.service.single;

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
import com.gz.gamecity.gameserver.table.LuckyWheelTable;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class LuckyWheelService implements LogicHandler{

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
		case Protocols.C2g_luckyWheel_enter.subCode_value:
			handleEnterRoom(player,cMsg);
			break;
		case Protocols.C2g_luckyWheel_bet.subCode_value:
			handleBet(player,cMsg);
			break;
		case Protocols.C2g_luckyWheel_leave.subCode_value:
			handleLeaveRoom(player,cMsg);
			break;
			
		}

	}
	
	
	private void handleLeaveRoom(Player player, ClientMsg cMsg) {
		Room room=RoomManager.getInstance().getRoom(RoomType.LuckyWheel);
		LuckyWheelTable table = (LuckyWheelTable)room.getTable(player.getTableId());
		
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
		GameTable t = RoomManager.getInstance().checkPlayerTable(player, RoomType.LuckyWheel);
		if(t==null){
			player.getChannel().close();
			return;
		}
		LuckyWheelTable table = (LuckyWheelTable)t;
		//返回获奖结果
		table.getReward(player, cMsg);
	}

	private void handleEnterRoom(Player player,ClientMsg cMsg) {
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_luckyWheel_enter.subCode_value);
		Room room=RoomManager.getInstance().enterRoom(player, RoomType.LuckyWheel);
		if(room==null){
			cMsg.put(Protocols.ERRORCODE, "进入房间失败");
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		LuckyWheelTable table=  new LuckyWheelTable(room);
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
		return Protocols.MainCode.LUCKYWHEEL;
	}

}
