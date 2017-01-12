package com.gz.gamecity.gameserver.service.fruit;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.room.FruitRoom;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.room.RoomManager;
import com.gz.gamecity.gameserver.room.RoomType;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.gameserver.table.FruitTable;
import com.gz.gamecity.gameserver.table.GameTable;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class FruitService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg bMsg) {
		Player player = null;
		if(!bMsg.isInner()){
			player=PlayerManager.getPlayerFromMsg(bMsg);
			if(player==null){
				bMsg.closeChannel();
				return;
			}
		}
		ClientMsg msg=(ClientMsg)bMsg;
		int subCode = msg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.C2g_fruit_enter.subCode_value:
			handleEnterRoom(player,msg);
			break;
		case Protocols.Inner_game_fruit_start_bet.subCode_value:
			handleStartBet();
			break;
		case Protocols.Inner_game_fruit_checkout.subCode_value:
			handleCheckout();
			break;
		case Protocols.C2g_fruit_bet.subCode_value:
			handleBet(player,msg);
			break;
		case Protocols.C2g_fruit_leave_table.subCode_value:
			handleReqLeaveTable(player,msg);
			break;
		default:
			break;
		}
	}

	private void handleReqLeaveTable(Player player, ClientMsg msg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Fruit);
		GameTable t = room.getTable(tableId);
		msg.put(Protocols.SUBCODE, Protocols.G2c_fruit_leave_table.subCode_value);
		if(t!=null){
			if(t.canLeave(player.getUuid())){
				t.playerLeave(player.getUuid());
				room.playerLeave(player);
			}else{
				msg.put(Protocols.ERRORCODE,"无法离开");
			}
		}else{
			msg.put(Protocols.ERRORCODE,"未知的错误");
		}
		PlayerMsgSender.getInstance().addMsg(msg);
		
	}

	private void handleBet(Player player, ClientMsg msg) {
		String tableId=player.getTableId();
		Room room = RoomManager.getInstance().getRoom(RoomType.Fruit);
		GameTable t = room.getTable(tableId);
		if(t!=null){
			FruitTable table = (FruitTable)t;
			table.putBet(player, msg);
		}
	}

	private void handleCheckout() {
		Room room = RoomManager.getInstance().getRoom(RoomType.Fruit);
		FruitTable table = ((FruitRoom)room).getTable();
		table.checkout();
	}

	private void handleStartBet() {
		Room room = RoomManager.getInstance().getRoom(RoomType.Fruit);
		FruitTable table = ((FruitRoom)room).getTable();
		table.gameStart();
	}

	private void handleEnterRoom(Player player, ClientMsg msg) {
		msg.put(Protocols.SUBCODE, Protocols.G2c_fruit_enter.subCode_value);
		Room room=RoomManager.getInstance().enterRoom(player, RoomType.Fruit);
		if(room==null){
			msg.put(Protocols.ERRORCODE, "进入房间失败");
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		FruitTable table = ((FruitRoom)room).getTable();
		if(!table.playerSitDown(player)){
			msg.put(Protocols.ERRORCODE, "穷逼，先充点钱再来玩");
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.FRUIT;
	}

}
