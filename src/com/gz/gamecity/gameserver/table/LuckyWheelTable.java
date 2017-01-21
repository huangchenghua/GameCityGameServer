package com.gz.gamecity.gameserver.table;

import java.util.HashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.ClientMsg;

public class LuckyWheelTable extends GameTable{
	private HashMap<String, Long> player_bet=new HashMap<>();
	private HashMap<String, Long> player_bet2=new HashMap<>();
	private static final int exp = 2;
	public LuckyWheelTable(Room room) {
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

	public void getReward(Player player,ClientMsg cMsg){
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_luckyWheel_reward.subCode_value);
		long bet=cMsg.getJson().getLongValue("bet");
		this.putBet(player, bet);
		//检查级别
		JSONArray levellist=AllTemplate.getLuckyWheel_level_jsonArray();//等级表
		JSONArray json = null;//概率表
			for(int i=0;i<levellist.size();i++){
				JSONObject jobj=(JSONObject)levellist.get(i);
				if(bet==Integer.parseInt(jobj.getString("bet_money"))){
					if(i==0){
						json=AllTemplate.getLuckyWheel_probobalitie1_jsonArray();
					}else if(i==1){
						json=AllTemplate.getLuckyWheel_probobalitie2_jsonArray();
					}else if(i==2){
						json=AllTemplate.getLuckyWheel_probobalitie3_jsonArray();
					}
				}
			}
			if(json==null){
				cMsg.put(Protocols.ERRORCODE, "数值错误");
			}
		int rd=(int)(Math.random()*10000)+1;
		
		int reward=0;
		for(int i=0;i<json.size();i++){
			JSONObject jobj=(JSONObject)json.get(i);
			if(rd>=Integer.parseInt(jobj.getString("startValue"))&&rd<=Integer.parseInt(jobj.getString("endValue"))){
				cMsg.put("id",Integer.parseInt(jobj.getString("id")));
				reward=Integer.parseInt(jobj.getString("prize"));
				putCoin(player, reward);
				cMsg.put("profit",reward);
				break;
			}
		}
		PlayerMsgSender.getInstance().addMsg(cMsg);
		PlayerDataService.getInstance().modifyCoin(player,reward-bet,EventLogType.luckywheel_bet);
		PlayerDataService.getInstance().addExp(player,exp);
	}
	public void putBet(Player player,long bet)
	{
		player_bet.put(player.getUuid(), bet);
	}
	public void putCoin(Player player,long coin)
	{
		player_bet2.put(player.getUuid(), coin);
	}

	@Override
	public void playerReconnect(Player player) {
		playerLeave(player.getUuid());
		room.playerLeave(player);
	}


}
