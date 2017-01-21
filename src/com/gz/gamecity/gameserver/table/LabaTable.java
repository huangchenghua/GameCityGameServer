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

public class LabaTable extends GameTable{

	public HashMap<String, Long> player_bet=new HashMap<>();
	
	public HashMap<String, Long> player_bet2=new HashMap<>();
	
	public HashMap<String, Integer> player_star=new HashMap<>();
	
	public LabaTable(Room room) {
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
		//  这里判断一下玩家是否有筹码还没有收取，需要返还筹码
		Player player = players.get(uuid);
		if(player!=null){
			if(player_bet2.get(uuid)!=null){
				long bet = player_bet2.get(uuid);
				PlayerDataService.getInstance().modifyCoin(player,bet,EventLogType.laba_checkout);
			}
		}
		super.playerLeave(uuid);
	}

	public void putBet(Player player,long bet)
	{
		player_bet.put(player.getUuid(), bet);
	}

	@Override
	public void playerReconnect(Player player) {
		playerLeave(player.getUuid());
		room.playerLeave(player);
	}

	//收分
	public void getPoint(Player player,ClientMsg cMsg){
		PlayerDataService.getInstance().modifyCoin(player,player_bet2.get(player.getUuid()),EventLogType.laba_checkout);
		
		player_star.put(player.getUuid(), 0);
		
		player_bet2.put(player.getUuid(), (long)0);
		
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_laba_getpoint.subCode_value);
		
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	
	//猜金币处理
	public void handleGuess(Player player,ClientMsg cMsg){
		int rate = cMsg.getJson().getIntValue("rate");
		long playerCash1 = player_bet2.get(player.getUuid())*rate;
		player_bet2.put(player.getUuid(),playerCash1);
		int star = 0;
		byte option = 0;
		
		boolean result_guess=handleGuessRandom(player,rate);
		
		if(result_guess == true){
			long playerCash2 = player_bet2.get(player.getUuid())*2;
			player_bet2.put(player.getUuid(),playerCash2);
			star = player_star.get(player.getUuid());
			star++;
			player_star.put(player.getUuid(), star);
			option = 1;
		}else if(result_guess == false){
			PlayerDataService.getInstance().modifyCoin(player,-player_bet2.get(player.getUuid()),EventLogType.laba_bet);
			option = 0;
		}
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_laba_guess.subCode_value);
		cMsg.put("option",option);
		cMsg.put("star", star);
	
		PlayerDataService.getInstance().addExp(player, 2);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	
	//猜金币随机处理
	private boolean handleGuessRandom(Player player,int rate){
		JSONArray labaSecondRate = AllTemplate.getLaba_probobality2_jsonArray() ;
		
		int judgeTargetNumber = 0;
		
		for(int i=0;i<labaSecondRate.size();i++){
			JSONObject labaSecondRateObj = (JSONObject)labaSecondRate.get(i);
			if(rate == labaSecondRateObj.getInteger("multiple")){
				judgeTargetNumber = labaSecondRateObj.getInteger("judgeValue");
				break;
			}
		}
		
		int judgeNumber = (int)(Math.random()*10000);
		
		if(judgeNumber>=judgeTargetNumber){
			return true;
		}else{
			return false;
		}	
	}

	//猜奖处理
	public void handleBet(Player player,ClientMsg cMsg){
		int option = handleRandom(player);
		
		PlayerDataService.getInstance().modifyCoin(player,-(player_bet.get(player.getUuid())),EventLogType.laba_bet);
		long reward = player_bet.get(player.getUuid()) * option;
		player_bet2.put(player.getUuid(), reward);
		
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_laba_bet.subCode_value);
		cMsg.put("option", option);
		cMsg.put("reward", reward);
		
		PlayerDataService.getInstance().addExp(player, 2);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	//随机处理
	private int handleRandom(Player player){
		// TODO 根据玩家下注的选项，随机出输赢		
		JSONArray labaLevel = AllTemplate.getLaba_level_jsonArray();
		JSONArray labaProbability = AllTemplate.getLaba_probobality1_jsonArray();
		JSONObject labaProbabilitySinglePreObj = null ;
		JSONArray labaProbabilitySingle = null;
		
		for(int i=0;i<labaLevel.size();i++){
			JSONObject labaLevelObj=(JSONObject)labaLevel.get(i);
			if(player.getCoin()>=Long.parseLong(labaLevelObj.getString("minCoin"))&&player.getCoin()<=Long.parseLong(labaLevelObj.getString("maxCoin"))){
				JSONArray coinLevel = (JSONArray)labaLevelObj.getJSONArray("betCoin");
				for(int j=0;j<coinLevel.size();j++){
					JSONObject coinLevelObj = (JSONObject)coinLevel.get(j);
					if(player_bet.get(player.getUuid()) == Integer.parseInt(coinLevelObj.getString("bet"))){
						labaProbabilitySinglePreObj = (JSONObject)labaProbability.get(j);
						labaProbabilitySingle = (JSONArray)labaProbabilitySinglePreObj.getJSONArray("probability"+Integer.toString(j+1));
						break;
					}
				}
			}
		}
		
		if(labaProbabilitySingle == null){
			player.getChannel().close();
		}
		
		int judgeNumber = (int)(Math.random()*10000);
		
		int odds = 0;
		
		for(int k=0;k<labaProbabilitySingle.size();k++){
			JSONObject labaProbabilitySingleObj=(JSONObject)labaProbabilitySingle.get(k);
			if((judgeNumber>=Integer.parseInt(labaProbabilitySingleObj.getString("startValue")))&&(judgeNumber<Integer.parseInt(labaProbabilitySingleObj.getString("endValue")))){
				odds = Integer.parseInt(labaProbabilitySingleObj.getString("odds"));
				break;
			}
		}
		
		return odds;
	}
}
