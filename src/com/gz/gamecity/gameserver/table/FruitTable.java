package com.gz.gamecity.gameserver.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.delay.DelayMsg;
import com.gz.gamecity.delay.InnerDelayManager;
import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.service.common.ChatService;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.JsonUtil;
import com.gz.websocket.msg.ClientMsg;

public class FruitTable extends GameTable{
	/**
	 * 可以下注的时间
	 */
	private static final long time_bet = (long) (15*1000l);
//	private static final long time_bet = (long) (2*60*1000l);
	
	/**
	 * 结算后开下一局的时间
	 */
	private static final long time_wait = (long) (10*1000l);

	private long next_event_time;
	
	private ConcurrentHashMap<String, HashMap<Integer, BetInfo>> bets_player=new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, HashMap<Integer, BetInfo>> bets_player_last=new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, Long> bets_table=new ConcurrentHashMap<>();
	
	private JSONObject json_table_bets=new JSONObject();
	
	private ArrayList<Integer> list_result=new ArrayList<>();
	
	private long remain=0l;
	
	private static final int exp = 3;
	
	private Pool pool=new Pool();
	
	public FruitTable(Room room) {
		super(room);
		gameStart();
	}

	@Override
	public void playerOffline(String uuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canLeave(String uuid) {
		Player player = players.get(uuid);
		if(player!=null){
			if(table_status == STATUS_ONGOING){
				if(bets_player.get(uuid)!=null){
					//如果已经下注了，就不能离开
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	protected boolean allowSitDown(Player player) {
		if(super.allowSitDown(player)){
			JSONObject j=AllTemplate.getFruit_level().getJSONObject(0);
			if(player.getCoin() >= j.getLongValue("min"))
				return true;
		}
		return false;
	}

	
	@Override
	public void playerLeave(String uuid) {
		//这里必须重写，因为父类的方法会将桌子从房间中移除
		Player player=players.remove(uuid);
		if(player!=null)
			player.setTableId(null);
	}
	
	@Override
	public boolean playerSitDown(Player player) {
		boolean result = super.playerSitDown(player);
		if(result){
			sendTableInfo(player);
		}
		return result;
	}
	
	private void sendTableInfo(Player player){
		ClientMsg msg=new ClientMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2c_fruit_enter.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2c_fruit_enter.subCode_value);
		long remain = next_event_time - System.currentTimeMillis();
		if(remain<0)remain=0;
		msg.put(Protocols.G2c_fruit_enter.REMAIN_TIMESTAMP, remain);
		msg.put(Protocols.G2c_fruit_enter.TABLESTATUS, table_status);
		msg.setChannel(player.getChannel());
		int[] results= new int[list_result.size()];
		for(int i=0;i<results.length;i++){
			results[i] = list_result.get(i);
		}
		msg.put(Protocols.G2c_fruit_enter.RESULTS, results);
		PlayerMsgSender.getInstance().addMsg(msg);
		
		HashMap<Integer, BetInfo> bets_mine = bets_player.get(player.getUuid());
		if(bets_mine!=null){
			JSONObject[] jsons_mine=new JSONObject[bets_mine.size()];
			int i = 0;
			for(Integer _id:bets_mine.keySet()){
				jsons_mine[i]=new JSONObject();
				jsons_mine[i].put(Protocols.G2c_fruit_bet_refresh.BetInfo.ID, _id);
				jsons_mine[i].put(Protocols.G2c_fruit_bet_refresh.BetInfo.BETS, bets_mine.get(_id).bet);
				i++;
			}
			ClientMsg msg_mine = new ClientMsg();
			msg_mine.put(Protocols.MAINCODE, Protocols.G2c_fruit_bet.mainCode_value);
			msg_mine.put(Protocols.SUBCODE, Protocols.G2c_fruit_bet.subCode_value);
			msg_mine.put(Protocols.G2c_fruit_bet.BETINFO, jsons_mine);
			msg_mine.setChannel(player.getChannel());
			PlayerMsgSender.getInstance().addMsg(msg_mine);
		}
	}
	
	@Override
	public void gameStart() {
		super.gameStart();
		bets_table.clear();
		bets_player_last.clear();
		bets_player_last.putAll(bets_player);
		bets_player.clear();
		json_table_bets.put(Protocols.MAINCODE, Protocols.G2c_fruit_bet_refresh.mainCode_value);
		json_table_bets.put(Protocols.SUBCODE, Protocols.G2c_fruit_bet_refresh.subCode_value);
		for(int i=0;i<8;i++){
			bets_table.put(i+1, 0l);
		}
		
		ClientMsg cMsg=new ClientMsg();
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_fruit_begin_bet.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_fruit_begin_bet.subCode_value);
		cMsg.put(Protocols.G2c_fruit_begin_bet.REMAIN_TIMESTAMP, time_bet);
		sendTableMsg(cMsg);
		
		DelayMsg msg = new DelayMsg(time_bet){
			@Override
			public void onTimeUp() {
				ClientMsg cMsg = new ClientMsg();
				cMsg.setMainCode(Protocols.Inner_game_fruit_checkout.mainCode_value);
				cMsg.put(Protocols.MAINCODE, Protocols.Inner_game_fruit_checkout.mainCode_value);
				cMsg.put(Protocols.SUBCODE, Protocols.Inner_game_fruit_checkout.subCode_value);
				cMsg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(cMsg);
			}
		};
		InnerDelayManager.getInstance().addDelayItem(msg);
		next_event_time = System.currentTimeMillis()+time_bet;
	}
	
	@Override
	public void gameEnd() {
		super.gameEnd();
		long delayTime = time_wait;
		if(list_result.size()>0){
			int index =list_result.get(list_result.size()-1);
			if(index>15 && index<21) //特殊奖客户端需要更多时间去表现
				delayTime += 5000;
		}
		
		DelayMsg msg = new DelayMsg(delayTime){
			@Override
			public void onTimeUp() {
				ClientMsg cMsg = new ClientMsg();
				cMsg.setMainCode(Protocols.Inner_game_fruit_start_bet.mainCode_value);
				cMsg.put(Protocols.MAINCODE, Protocols.Inner_game_fruit_start_bet.mainCode_value);
				cMsg.put(Protocols.SUBCODE, Protocols.Inner_game_fruit_start_bet.subCode_value);
				cMsg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(cMsg);
			}
		};
		InnerDelayManager.getInstance().addDelayItem(msg);
		next_event_time = System.currentTimeMillis()+time_wait;
		
	}
	
	public void checkout(){
		try {
			handleResult();
			sendResult();
			settlement();
			addExp();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		gameEnd();
	}
	
	private void addExp() {
		for(String uuid:bets_player.keySet()){
			Player player = players.get(uuid);
			if(player!=null){
				PlayerDataService.getInstance().addExp(player, exp);
			}
		}
	}

	private void settlement() {
		JSONArray json_odds_arr = AllTemplate.getFruit_odds();
		int index = list_result.get(list_result.size()-1);
		JSONObject json_result_odds = json_odds_arr.getJSONObject(index);
		long reward_all = 0l;
		for(String uuid:bets_player.keySet()){
			Player player = players.get(uuid);
			if(player==null)
				continue;
			HashMap<Integer, BetInfo> map_bets = bets_player.get(uuid);
			long reward = getPlayerReward(json_result_odds,map_bets);
			if(reward>0)
				PlayerDataService.getInstance().modifyCoin(player, reward,EventLogType.fruit_checkout);
			reward_all+=reward;
		}
		if(index  == 20){ //出了大满贯要全服通知
			StringBuffer sb = new StringBuffer();
			if(reward_all == 0){
				sb.append(AllTemplate.getGameString("str22"));
			}else{
				sb.append(AllTemplate.getGameString("str23"));
				sb.append(reward_all);
				sb.append(AllTemplate.getGameString("str24"));
			}
			
			ChatService.getInstance().sendGameMsg(sb.toString());
		}
		
	}

	/**
	 * 开奖
	 */
	private void handleResult() {
//		if(1==1){
//			list_result.add(0);
//			return;
//		}
		if(list_result.size()>=6){
			list_result.remove(0);
		}
		JSONArray json_odds_arr = AllTemplate.getFruit_odds();
		
		long bet_all=0l;
		for(Integer _index:bets_table.keySet()){
			bet_all = bet_all + bets_table.get(_index);
		}
		//如果没有人押注，就随机开了
		if(bet_all == 0){
			int index_result = new Random().nextInt(json_odds_arr.size());
			list_result.add(index_result);
			return;
		}
		//存每个选项要给玩家多少奖励
		HashMap<Integer,OptionReward> map_rewards_option=new HashMap<>();
		//估算每个选项可能出的奖金
		//两层循环，计算所有玩家在每个选项中奖的金额
		for(String uuid:bets_player.keySet()){
			Player player = players.get(uuid);
			if(player==null)
				continue;
			HashMap<Integer, BetInfo> map_bets = bets_player.get(uuid);
			for(int i = 0;i<json_odds_arr.size();i++){
				JSONObject j = json_odds_arr.getJSONObject(i);
				long reward = getPlayerReward(j,map_bets);
				
				OptionReward reward_opt=map_rewards_option.get(j.getIntValue("index"));
				if(reward_opt==null)
				{
					reward_opt =new OptionReward(j.getIntValue("index"),reward);
				}else{
					reward_opt.reward = reward_opt.reward+reward;
				}
				map_rewards_option.put(j.getIntValue("index"), reward_opt);
			}
			
		}
		
		long line_top = (long)(bet_all *90/100) + remain;
		long line_bottom=0l;
		boolean openWater = pool.canOpenWater();
		if(openWater){
			line_bottom = line_top;
			line_top = (long) (line_top * 5);
		}
		ArrayList<OptionReward> list_include=new ArrayList<>();
		ArrayList<OptionReward> list_exclude=new ArrayList<>();
		for(Integer _index:map_rewards_option.keySet()){
			if(map_rewards_option.get(_index).reward<=line_top && map_rewards_option.get(_index).reward>=line_bottom)
				list_include.add(map_rewards_option.get(_index));
			else
				list_exclude.add(map_rewards_option.get(_index));
		}
		
		//如果没有符合条件的开奖结果,就直接开LUCKY
		int index_result=0;
		if(list_include.size()<1){
			list_result.add(json_odds_arr.size()-1);//最后一个是LUCKY
		}else{
			int index = new Random().nextInt(list_include.size());
			list_result.add(index);
		}
		
		
		
		OptionReward reward_selected = list_include.get(index_result);
		remain = (line_top - reward_selected.reward)/2;
		if(remain<0)
			remain=0;
		pool.addWater(remain/2);
		remain = remain/2;
		
	}
	
	private void sendResult()
	{
		long delayTime = time_wait;
		if(list_result.size()>0){
			int index =list_result.get(list_result.size()-1);
			if(index>15 && index<21) //特殊奖客户端需要更多时间去表现
				delayTime += 5000;
		}
		// 发包
		ClientMsg msg_table_result = new ClientMsg();
		msg_table_result.put(Protocols.MAINCODE, Protocols.G2c_fruit_result.mainCode_value);
		msg_table_result.put(Protocols.SUBCODE, Protocols.G2c_fruit_result.subCode_value);
		msg_table_result.put(Protocols.G2c_fruit_result.OPTION, list_result.get(list_result.size() - 1));
		msg_table_result.put(Protocols.G2c_fruit_result.REMAIN_TIMESTAMP, delayTime);
		
		
		sendTableMsg(msg_table_result);
		
		
	}
	
	private static int getRandomIndex(int[] options_Exclude){
		JSONArray json_odds_arr = AllTemplate.getFruit_odds();
		int index = 21;
		if(options_Exclude ==null || options_Exclude.length==0)
		{
			index = new Random().nextInt(json_odds_arr.size());
		}else{
			int[] tmp = new int[json_odds_arr.size()-options_Exclude.length];
			for(int i=0,j=0;i<json_odds_arr.size();i++){
				boolean exist=false;
				for(int n=0;n<options_Exclude.length;n++){
					if(i == options_Exclude[n]){
						exist=true;
						break;
					}
				}
				if(!exist){
					tmp[j]=i;
					j++;
				}
			}
			index = tmp[new Random().nextInt(tmp.length)];
			
		}
		
		
		
		return index;
	}
	
//	public static void main(String[] args) {
//		AllTemplate.initTemplates();
//		HashMap<Integer, Integer> map=new HashMap<>();
//		int[] options_Exclude = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21};
//		for(int i=0;i<1000;i++){
//			int index = getRandomIndex(options_Exclude);
//			Integer ii=map.get(index);
//			if(ii==null)
//			{
//				ii = new Integer(1);
//			}else{
//				ii = ii.intValue()+1;
//			}
//			map.put(index, ii);
//		}
//		for(Integer index:map.keySet()){
//			System.out.println(index+"  :  "+map.get(index).intValue());
//		}
//	}
	
	
	private long getPlayerReward(JSONObject json_odds, HashMap<Integer, BetInfo> map_bets){
		if(json_odds.getIntValue("odds")==0)
			return 0l;
		long reward=0l;
		JSONArray children_array = json_odds.getJSONArray("index_children");
		if(children_array !=null){ //组合奖
			//判断有没有几个都下注了
			boolean flag=true;
			for(int i=0;i<children_array.size();i++){
				int child_index = children_array.getIntValue(i);
				JSONObject json_odds_child = AllTemplate.getFruit_odds().getJSONObject(child_index);
				int id = json_odds_child.getIntValue("id");
				if(!map_bets.containsKey(id)){
					flag = false;
					break;
				}
			}
			
			for(int i=0;i<children_array.size();i++){
				int child_index = children_array.getIntValue(i);
				JSONObject json_odds_child = AllTemplate.getFruit_odds().getJSONObject(child_index);
				int id = json_odds_child.getIntValue("id");
				BetInfo betInfo = map_bets.get(id);
				if(betInfo!=null){
					if(flag){
						reward += (betInfo.bet*json_odds.getIntValue("odds"));
					}else{
						reward += (betInfo.bet*json_odds_child.getIntValue("odds"));
					}
				}
				
			}
		}
		else{
			int id = json_odds.getIntValue("id");
			BetInfo betInfo = map_bets.get(id);
			if(betInfo!=null){
				reward += (betInfo.bet*json_odds.getIntValue("odds"));
			}
		}
		return reward;
	}
	
	public void rebet(Player player,ClientMsg msg)
	{
		msg.put(Protocols.SUBCODE, Protocols.G2c_fruit_bet.subCode_value);
		//如果已经下过注了就不能使用重复下注
		HashMap<Integer, BetInfo> bet_player = bets_player.get(player.getUuid());
		if(bet_player!=null)
		{
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str25"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		//如果前一把没有下注就返回
		bet_player = bets_player_last.get(player.getUuid());
		if(bet_player==null)
		{
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str26"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		//计算当前游戏币是否足够重复下注
		long bet = 0l;
		for (BetInfo betInfo:bet_player.values()) {
			bet = bet + betInfo.bet;
		}
		if(bet>player.getCoin()){
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str27"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		bets_player.put(player.getUuid(), bet_player);
		PlayerDataService.getInstance().modifyCoin(player, -bet, EventLogType.fruit_bet);
		sendMyBetInfo(player);
		for (BetInfo betInfo:bet_player.values()) {
			try {
				long bets = bets_table.get(betInfo.id);
				bets += betInfo.bet;
				bets_table.put(betInfo.id, bets);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		sendTableBetInfo();
	}
	

	public void putBet(Player player,ClientMsg msg){
		int id = msg.getJson().getIntValue(Protocols.C2g_fruit_bet.ID);
		int bet_index = msg.getJson().getIntValue(Protocols.C2g_fruit_bet.BET_INDEX);
		if(id<1 || id>8 || bet_index<0 || bet_index>4){
			player.getChannel().close();
			return;
		}
		long bet=0;
		JSONArray ja = AllTemplate.getFruit_level();
		for(int i=0;i<ja.size();i++){
			JSONObject j = ja.getJSONObject(i);
			if(player.getCoin()>=j.getLongValue("min") && player.getCoin()<=j.getLongValue("max")){
				JSONArray arr = j.getJSONArray("bets");
				bet = arr.getLongValue(bet_index);
			}
		}
		if(bet==0){ //表示没找到对应的配置
			player.getChannel().close();
			return;
		}
		PlayerDataService.getInstance().modifyCoin(player, -bet,EventLogType.fruit_bet);
		try {
			long bets = bets_table.get(id);
			bets += bet;
			bets_table.put(id, bets);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
//		BetInfo betInfo=new BetInfo(player, id, bet);
		HashMap<Integer, BetInfo> bets_mine = bets_player.get(player.getUuid());
		if(bets_mine==null)
		{
			bets_mine = new HashMap<>();
			bets_player.put(player.getUuid(), bets_mine);
		}
		BetInfo betInfo = bets_mine.get(id);
		if(betInfo==null){
			betInfo =new BetInfo(player, id, bet);
			bets_mine.put(id, betInfo);
		}else{
			long _bet=betInfo.bet+bet;
			betInfo.bet = _bet;
		}
		sendMyBetInfo(player);
		sendTableBetInfo();
		
	}
	
	private void sendMyBetInfo(Player player){
		HashMap<Integer, BetInfo> bets_mine = bets_player.get(player.getUuid());
		JSONObject[] jsons_mine=new JSONObject[bets_mine.size()];
		int i = 0;
		for(Integer _id:bets_mine.keySet()){
			jsons_mine[i]=new JSONObject();
			jsons_mine[i].put(Protocols.G2c_fruit_bet_refresh.BetInfo.ID, _id);
			jsons_mine[i].put(Protocols.G2c_fruit_bet_refresh.BetInfo.BETS, bets_mine.get(_id).bet);
			i++;
		}
		ClientMsg msg_mine = new ClientMsg();
		msg_mine.put(Protocols.MAINCODE, Protocols.G2c_fruit_bet.mainCode_value);
		msg_mine.put(Protocols.SUBCODE, Protocols.G2c_fruit_bet.subCode_value);
		msg_mine.put(Protocols.G2c_fruit_bet.BETINFO, jsons_mine);
		msg_mine.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg_mine);
	}
	
	private void sendTableBetInfo(){
		JSONObject[] jsons=new JSONObject[bets_table.size()];
		int i=0;
		for(Integer _id:bets_table.keySet()){
			jsons[i]=new JSONObject();
			jsons[i].put(Protocols.G2c_fruit_bet_refresh.BetInfo.ID, _id);
			jsons[i].put(Protocols.G2c_fruit_bet_refresh.BetInfo.BETS, bets_table.get(_id));
			i++;
		}
		json_table_bets.put(Protocols.G2c_fruit_bet_refresh.BETINFO, jsons);
		ClientMsg cMsg=new ClientMsg();
		cMsg.setJson(JsonUtil.copyJson(json_table_bets));
		sendTableMsg(cMsg);
	}

	@Override
	public void playerReconnect(Player player) {
		sendTableInfo(player);
		
	}

	@Override
	public void closeTable() {
		// TODO Auto-generated method stub
		
	}
}

class OptionReward{
	public int index;
	public long reward;
	public OptionReward(int index,long reward){
		this.index = index;
		this.reward = reward;
	}
}
