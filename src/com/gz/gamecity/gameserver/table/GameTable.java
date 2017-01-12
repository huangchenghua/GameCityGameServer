package com.gz.gamecity.gameserver.table;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.ClientMsg;

public abstract class GameTable {
	
	public static final byte STATUS_WAITING=0;
	public static final byte STATUS_ONGOING=1;
	
	protected ConcurrentHashMap<String, Player>  players;
	protected Room room;
	protected String tableId;
	protected byte table_status;
	public String getTableId() {
		return tableId;
	}

	public byte getTable_status() {
		return table_status;
	}

	public Room getRoom() {
		return room;
	}

	public GameTable(Room room) {
		this.tableId = UUID.randomUUID().toString();
		this.room = room;
		this.table_status = STATUS_WAITING;
		players =new ConcurrentHashMap<>();
	}
	
	public boolean playerSitDown(Player player){
		if(!allowSitDown(player))
			return false;
		if(!players.containsKey(player.getUuid())){
			players.put(player.getUuid(), player);
			player.setTableId(tableId);
			return true;
		}
		return false;
	}
	
	/**
	 * 判断玩家是否够条件玩
	 * @param player
	 * @return
	 */
	protected boolean allowSitDown(Player player){
		for(Player p:players.values()){
			if(p.getUuid().equals(player.getUuid()))
				return false;
		}
		return true;
	}
	
	/**
	 * 玩家掉线
	 * @param uuid
	 */
	public abstract void playerOffline(String uuid);
	
	/**
	 * 玩家离开桌子
	 * @param uuid
	 */
	public void playerLeave(String uuid){
		Player player=players.remove(uuid);
		if(player!=null)
			player.setTableId(null);
		if(players.isEmpty()){
			room.removeTable(tableId);
		}
	}
	
	public void sendTableMsg(ClientMsg msg){
		for(Player player:players.values()){
			if(player.isOnline()){
				ClientMsg cMsg = msg.copy();
				cMsg.setChannel(player.getChannel());
				PlayerMsgSender.getInstance().addMsg(cMsg);
			}
		}
	}
	
	public void gameStart(){
		this.table_status = STATUS_ONGOING;
	}
	
	public void gameEnd(){
		this.table_status = STATUS_WAITING;
		for(Player player:players.values()){
			if(!player.isOnline()){
				playerLeave(player.getUuid());
				player.setTableId(null);
				ClientMsg msg=new ClientMsg();
				msg.setMainCode(Protocols.Inner_game_player_logout.mainCode_value);
				msg.put(Protocols.MAINCODE, Protocols.Inner_game_player_logout.mainCode_value);
				msg.put(Protocols.SUBCODE, Protocols.Inner_game_player_logout.subCode_value);
				msg.put(Protocols.Inner_game_player_logout.UUID, player.getUuid());
				msg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(msg);
			}
		}
	}
	
	public abstract boolean canLeave(String uuid);

	public int getPlayerCount(){
		return players.size();
	}
	
	public abstract void playerReconnect(Player player);

	public boolean existPlayer(Player player) {
		for(Player p:players.values()){
			if(p==player)
				return true;
		}
		return false;
	}
	
}

