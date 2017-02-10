package com.gz.gamecity.gameserver.table;

import java.util.HashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.ClientMsg;

public class BlackATable extends GameTable{
	public HashMap<String, Long> player_bet=new HashMap<>();
	public HashMap<String, Long> player_reward=new HashMap<>();
	public HashMap<String, Integer> player_star=new HashMap<>();
	private static final int exp = 2;
	public BlackATable(Room room) {
		super(room);
	}

	@Override
	protected boolean allowSitDown(Player player) {
		if(super.allowSitDown(player))
			if(player.getCoin()>100)
				return true;
		return false;
	}

	@Override
	public void playerOffline(String uuid) {
		
		
	}

	@Override
	public boolean canLeave(String uuid) {
		
		return true;
	}

	@Override
	public void playerLeave(String uuid) {
		// 这里判断一下玩家是否有筹码还没有收取，需要返还筹码
		Player player = players.get(uuid);
		if(player!=null){
			long bet = player_reward.get(uuid);
			PlayerDataService.getInstance().modifyCoin(player,bet,EventLogType.blackA_bet);
		}
		super.playerLeave(uuid);
		
//		if(player_reward.get(uuid)==null||PlayerManager.getInstance().getOnlinePlayer(uuid)==null){
//			return ;
//		}
//		PlayerDataService.getInstance().modifyCoin(PlayerManager.getInstance().getOnlinePlayer(uuid),player_reward.get(uuid));
	}

	public void putBet(Player player,ClientMsg cMsg){
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_blackA_reward.subCode_value);
		long bet=cMsg.getJson().getIntValue("bet");
		//押注金额保存
		if(player_bet.get(player.getUuid())==0){
			player_bet.put(player.getUuid(), bet);
		}
		int star=player_star.get(player.getUuid());	
		//检查级别
		JSONArray json=AllTemplate.getSpadeA_probobality_jsonArray();//概率表

		//随机概率
		int rd=(int)(Math.random()*10000)+1;
		int odd=0;
		long change=0;
		for(int i=0;i<json.size();i++){
			JSONObject jobj=(JSONObject)json.get(i);
			if(rd>=Integer.parseInt(jobj.getString("startValue"))&&rd<=Integer.parseInt(jobj.getString("endValue"))){
				odd=Integer.parseInt(jobj.getString("odds"));
				change=odd*bet;
				if(odd==2){
						long reward=player_reward.get(player.getUuid());
						reward=reward+change-bet;
						player_reward.put(player.getUuid(), reward);
					star++;
				}else{
					//star=0;
					long pbet=player_bet.get(player.getUuid());
					PlayerDataService.getInstance().modifyCoin(player, -pbet,EventLogType.blackA_bet);
				
					player_reward.put(player.getUuid(), (long)0);
					player_bet.put(player.getUuid(), bet);
				}
				putStar(player, star);
				cMsg.put(Protocols.G2c_blackA_reward.RESULT,Integer.parseInt(jobj.getString("odds")));
				cMsg.put(Protocols.G2c_blackA_reward.REWARD,change);
				cMsg.put(Protocols.G2c_blackA_reward.STAR,star);
				break;
			}
		}
		if(star==8){
			player_star.put(player.getUuid(),0);
			player_bet.put(player.getUuid(),(long)0);
			PlayerDataService.getInstance().modifyCoin(player, (long)player_reward.get(player.getUuid()),EventLogType.blackA_bet);
		}
		PlayerDataService.getInstance().addExp(player, exp);
		
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	public void putStar(Player player,int star){
		player_star.put(player.getUuid(), star);
	}

	@Override
	public void playerReconnect(Player player) {
		playerLeave(player.getUuid());
		room.playerLeave(player);
	}

	@Override
	public void closeTable() {
		// TODO Auto-generated method stub
		
	}
	
	

}
