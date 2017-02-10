package com.gz.gamecity.gameserver.table;

import java.lang.reflect.Array;
import java.util.ArrayList;
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

public class MahjongTable extends GameTable{

	public HashMap<String, Long> player_bet=new HashMap<>();
	
	public HashMap<String, Long> player_bet2=new HashMap<>();
	
	public HashMap<String, ArrayList> card_list = new HashMap<>();
	
	public HashMap<String, Integer> player_probability = new HashMap<>();
	
	public HashMap<String, Double> system_probability = new HashMap<>();
	
	public HashMap<String, Integer> player_star=new HashMap<>();
	
	public HashMap<String, Boolean> lock=new HashMap<>();
	
	public MahjongTable(Room room) {
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
			long bet = player_bet2.get(uuid);
			PlayerDataService.getInstance().modifyCoin(player,bet,EventLogType.mahjong_checkout);
		}
		super.playerLeave(uuid);
//		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
//		if( !(player_bet2.get(uuid) == null)){
//			PlayerDataService.getInstance().modifyCoin(player,player_bet2.get(uuid));
//		}
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

	//进入房间
	public void enterRoom(Player player,ClientMsg cMsg){
		ArrayList card_number = new ArrayList();
		for(int i=0;i<=9;i++){
			card_number.add(i,i);
		}
		
		player_bet2.put(player.getUuid(), (long)0);
		
		card_list.put(player.getUuid(), card_number);
		
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	
	//开始游戏
	public void startGame(Player player,ClientMsg cMsg){
		JSONArray mahjongLevel = AllTemplate.getMahjong_level_jsonArray();
		
		long coin = player.getCoin();
		
		long award = 0;
		
		JSONObject mahjongLevelObj = null; 
		
		for(int i=0;i<mahjongLevel.size();i++){
			mahjongLevelObj = (JSONObject)mahjongLevel.get(i);
			if((coin>=mahjongLevelObj.getLongValue("minCoin"))&&(coin<mahjongLevelObj.getLongValue("maxCoin"))){
				award = mahjongLevelObj.getLongValue("award");
			}
		}
		
		player_bet2.put(player.getUuid(), award);
		
		system_probability.put(player.getUuid(), 2.5);
		
		PlayerDataService.getInstance().modifyCoin(player,-player_bet2.get(player.getUuid()),EventLogType.mahjong_bet);
	}
	
	//收分
	public void getPoint(Player player,ClientMsg cMsg){
		
		PlayerDataService.getInstance().modifyCoin(player,player_bet2.get(player.getUuid()),EventLogType.mahjong_checkout);
		
		player_bet2.put(player.getUuid(), (long)0);
		
		ArrayList card_number = new ArrayList();
		for(int i=0;i<=9;i++){
			card_number.add(i,i);
		}
		card_list.put(player.getUuid(), card_number);
		
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_mahjong_getpoint.subCode_value);
		
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	//看牌
	public void getView(Player player, ClientMsg cMsg){
		ArrayList array = new ArrayList();
		
		boolean win;
		
		if((int)(Math.random()*100)>=50){
			win = true;
		}else{
			win = false;
		}
		
		array = randomCard(win,player);
		cMsg.put(Protocols.G2c_mahjong_getview.RESULT, array);
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_mahjong_getview.subCode_value);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	
	//麻将牌处理
	public void handleRandom(Player player,ClientMsg cMsg){
		
		int rate = player_probability.get(player.getUuid());
		
		if(rate != 1){
			long tempCashChange = player_bet2.get(player.getUuid())*(rate-1);
			PlayerDataService.getInstance().modifyCoin(player,-tempCashChange,EventLogType.mahjong_bet);
		}
		player_bet2.put(player.getUuid(), player_bet2.get(player.getUuid())*rate);
		
		// TODO Auto-generated method stub
		JSONArray mahjongProbability1 = AllTemplate.getMahjong_probability1_jsonArray();
		JSONArray mahjongProbability2 = AllTemplate.getMahjong_probability2_jsonArray();
		JSONObject mahjongProbability2Obj = null ;
		JSONObject mahjongProbability1Obj = null;
		JSONArray probabilityArray;
		int judgeTargetNumber = 0;
		for(int i=0;i<mahjongProbability2.size();i++){
			mahjongProbability2Obj = (JSONObject)mahjongProbability2.get(i);
			if((card_list.get(player.getUuid()).size())==Integer.parseInt(mahjongProbability2Obj.getString("card_num"))){
				for(int j=0;j<mahjongProbability1.size();j++){
					mahjongProbability1Obj = (JSONObject)mahjongProbability1.getJSONObject(j);
					if(player_probability.get(player.getUuid())==Integer.parseInt(mahjongProbability1Obj.getString("probability"))){
						probabilityArray=mahjongProbability2Obj.getJSONArray("winValue");
						judgeTargetNumber = probabilityArray.getInteger(j);
					}
				}
			}
		}
		
		ArrayList array = new ArrayList();

		int judgeNumber = (int)(Math.random()*10000);
		if(judgeNumber<judgeTargetNumber){
			//赢
			array = randomCard(true,player);
			
			long award = player_bet2.get(player.getUuid());
			
			award = (long)(system_probability.get(player.getUuid())*award);
			
			player_bet2.put(player.getUuid(), award);
			
			cMsg.put(Protocols.G2c_mahjong_bet.RESULT, array);
			cMsg.put(Protocols.G2c_mahjong_bet.WIN, true);
		}else{
			//输
			array = randomCard(false,player);
			
			long award = player_bet2.get(player.getUuid());
			
			PlayerDataService.getInstance().modifyCoin(player,-award,EventLogType.mahjong_bet);
			
			cMsg.put(Protocols.G2c_mahjong_bet.WIN, false);
			if(lock.get(player.getUuid())==true){
				cMsg.put(Protocols.G2c_mahjong_bet.RESULT, array);
			}else if(lock.get(player.getUuid())==false){
				cMsg.put(Protocols.G2c_mahjong_bet.RESULT, array);
				system_probability.put(player.getUuid(),system_probability.get(player.getUuid()) - 0.25);
				ArrayList tempArray = new ArrayList();
				tempArray = card_list.get(player.getUuid());
				for(int i=0;i<tempArray.size();i++){
					if(tempArray.get(i) == array.get(0)){
						tempArray.remove(i);
					}
				}
			}
		}
		PlayerDataService.getInstance().addExp(player, 2);
		cMsg.put(Protocols.SUBCODE,Protocols.G2c_mahjong_bet.subCode_value);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	
	
	//获取随机数组
	private ArrayList randomCard(boolean win , Player player){
		ArrayList _temp = card_list.get(player.getUuid());
		
		ArrayList tempArray = new ArrayList<>();
		tempArray.addAll(_temp);
		
		if(tempArray.size()<=4){
			player.getChannel().close();
		}
		
		ArrayList returnArray = new ArrayList();
		if(win == true){
			int winTempNum = (int)(Math.random()*4);
			returnArray.add(0,(int)tempArray.get(winTempNum));
			tempArray.remove(winTempNum);
		}else if(win == false){
			int loseTempNum = (int)(Math.random()*((int)(tempArray.size()-4))+4);
			returnArray.add(0,(int)tempArray.get(loseTempNum));
			tempArray.remove(loseTempNum);
		}
		int j=1;
		for(int i = tempArray.size();i>0;i--){
			int k = (int)(Math.random()*i);
			int target = (int)tempArray.get(k);
			tempArray.remove(k);
			returnArray.add(j, target);
			j++;
		}
		return returnArray;
	}

	@Override
	public void closeTable() {
		// TODO Auto-generated method stub
		
	}
}