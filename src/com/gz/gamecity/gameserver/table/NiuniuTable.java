package com.gz.gamecity.gameserver.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.delay.DelayMsg;
import com.gz.gamecity.delay.InnerDelayManager;
import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.service.common.ChatService;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.gameserver.service.niuniu.Const.GameCardType;
import com.gz.gamecity.gameserver.service.niuniu.NiuniuPoker;
import com.gz.gamecity.gameserver.service.niuniu.PokerCommon;
import com.gz.gamecity.gameserver.service.niuniu.Utils;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.Config;
import com.gz.util.JsonUtil;
import com.gz.websocket.msg.ClientMsg;


public class NiuniuTable extends GameTable {
	
	private static final int exp = 8;

	private static final Logger log=Logger.getLogger(NiuniuTable.class);
	
	private static final Pool pool=new Pool();
	/**
	 * 桌子上允许的最多人数
	 */
	private static final int maxPlayer=20;
	
	/**
	 * 可以下注的时间
	 */
	private static final long time_bet = Config.instance().getLValue(ConfigField.NIUNIU_TIME_BET);
	
	/**
	 * 结算后开下一局的时间
	 */
	private static final long time_wait = (long) (10*1000l);
	
	private static final int TIAN=1;
	private static final int DI=2;
	private static final int XUAN=3;
	private static final int HUANG=4;
	
	/**
	 * 抽成
	 */
	private float rate_commission=0.9f;
	
	
	private long next_event_time;
	
	private ConcurrentHashMap<String, BetInfo> bets_player=new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, ConcurrentHashMap<String, BetInfo>> bets_table=new ConcurrentHashMap<>();
	
	/**
	 * 牌桌上的五副牌，按照大到小排序
	 */
	private ArrayList<NiuniuPoker> poker_table=new ArrayList<>();
	
	
	private int lvl;
	
	private ArrayList<JSONObject> list_json;
	
	private Player banker_system;
	
	private JSONObject[] resultInfo;
	
	private LinkedList<String> list_req_banker=new LinkedList<>();
	
	private HashMap<String, Player> players_coin_rank=new HashMap<>();
	
	private JSONObject json_player_list = new JSONObject();
	
	private JSONObject json_banker_info = new JSONObject();
	
	private JSONObject json_bets = new JSONObject();
	
	/**
	 * 庄家的牌
	 */
	private NiuniuPoker poker_banker;
	
	public ArrayList<JSONObject> getList_json() {
		return list_json;
	}

	public void setList_json(ArrayList<JSONObject> list_json) {
		this.list_json = list_json;
		rate_commission = list_json.get(0).getFloatValue("rate_commission");
	}

	private Player banker;
	

	public int getLvl() {
		return lvl;
	}

	public void setLvl(int lvl) {
		this.lvl = lvl;
	}

	public NiuniuTable(Room room) {
		super(room);
		banker_system = new Player();
		banker_system.setName("系统");
		banker_system.setHead(0);
		banker_system.setUuid("1234655555");
		banker_system.setCoin(99999999999l);
		json_player_list.put(Protocols.MAINCODE, Protocols.G2c_niuniu_playerlist.mainCode_value);
		json_player_list.put(Protocols.SUBCODE, Protocols.G2c_niuniu_playerlist.subCode_value);
		json_bets.put(Protocols.MAINCODE, Protocols.G2c_niuniu_bet_refresh.mainCode_value);
		json_bets.put(Protocols.SUBCODE, Protocols.G2c_niuniu_bet_refresh.subCode_value);
		refreshBanker();
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
				if(player == banker)//庄家不能走
					return false;
				if(bets_player.get(uuid)!=null){
					//如果已经下注了，就不能离开
					return false;
				}
			}
		}
		return true;
	}

	public boolean isFull(){
		if(this.players.size()>=maxPlayer)
			return true;
		return false;
	}
	
	
	@Override
	public boolean playerSitDown(Player player) {
		if(super.playerSitDown(player)){
			sendTableInfo(player,false);
			
			return true;
		}
		return false;
		
	}
	
	
	private void sendTableInfo(Player player,boolean isReconnect){
		ClientMsg msg=new ClientMsg();
		msg.setChannel(player.getChannel());
		msg.put(Protocols.MAINCODE, Protocols.G2c_niuniu_tableInfo.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_tableInfo.subCode_value);
		msg.put(Protocols.G2c_niuniu_tableInfo.LVL, lvl);
		msg.put(Protocols.G2c_niuniu_tableInfo.TABLESTATUS, table_status);
		msg.put(Protocols.G2c_niuniu_tableInfo.ISRECONNECT, isReconnect);
		long remain = next_event_time - System.currentTimeMillis();
		if(remain<0)remain=0;
		msg.put(Protocols.G2c_niuniu_tableInfo.REMAIN_TIMESTAMP, remain);
		PlayerMsgSender.getInstance().addMsg(msg);
		if(table_status == STATUS_ONGOING){
			ClientMsg msg_bets = new ClientMsg();
			msg_bets.setJson(JsonUtil.copyJson(json_bets));
			msg_bets.setChannel(player.getChannel());
			PlayerMsgSender.getInstance().addMsg(msg_bets);
		}else if(table_status == STATUS_WAITING){
			ClientMsg msg_result = new ClientMsg();
			msg_result.put(Protocols.MAINCODE, Protocols.G2c_niuniu_result.mainCode_value);
			msg_result.put(Protocols.SUBCODE, Protocols.G2c_niuniu_result.subCode_value);
			msg_result.put(Protocols.G2c_niuniu_result.RESULTINFO, JsonUtil.copyJsonArray(resultInfo));
			msg_result.setChannel(player.getChannel());
			PlayerMsgSender.getInstance().addMsg(msg_result);
		}
		
		ClientMsg msg_banker=new ClientMsg();
		msg_banker.setJson(JsonUtil.copyJson(json_banker_info));
		msg_banker.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg_banker);
		ClientMsg msg_players = new ClientMsg();
		msg_players.setJson(JsonUtil.copyJson(json_player_list) );
		msg_players.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg_players);
		ClientMsg msg_bets = new ClientMsg();
		msg_bets.setJson(JsonUtil.copyJson(json_bets));
		PlayerMsgSender.getInstance().addMsg(msg_bets);
		BetInfo myBet = bets_player.get(player.getUuid());
		if(myBet!=null){
			ClientMsg msg_my_bet = new ClientMsg();
			msg_my_bet.put(Protocols.MAINCODE, Protocols.G2c_niuniu_bet.mainCode_value);
			msg_my_bet.put(Protocols.SUBCODE, Protocols.G2c_niuniu_bet.subCode_value);
			msg_my_bet.put(Protocols.G2c_niuniu_bet.ID, myBet.id);
			msg_my_bet.put(Protocols.G2c_niuniu_bet.BETS, myBet.bet);
			msg_my_bet.setChannel(player.getChannel());
			PlayerMsgSender.getInstance().addMsg(msg_my_bet);
		}
		
		
	}
	
	/**
	 * 下注
	 * @param player
	 * @param msg
	 */
	public void putBet(Player player, ClientMsg msg) {
		//庄家不能下注
		if(player.getUuid().equals(banker.getUuid())){
			player.getChannel().close();
			return;
		}
		int id = msg.getJson().getIntValue(Protocols.C2g_niuniu_bet.ID);
		int bet_index = msg.getJson().getIntValue(Protocols.C2g_niuniu_bet.BET_INDEX);
		if (id < TIAN || id > HUANG) {
			player.getChannel().close();
			return;
		}
		long bet=0;
		for(JSONObject json:list_json){
			if(player.getCoin()>=json.getLongValue("min") && player.getCoin()<=json.getLongValue("max")){
				JSONArray bets = json.getJSONArray("bets");
				if(bet_index<0 || bet_index >=bets.size()){
					player.getChannel().close();
					return;
				}
				bet = bets.getLongValue(bet_index);
			}
		}
		//有可能之前赢了，超过了房间进入的金币上限，所以找不到对应的筹码
		if(bet == 0){
			JSONObject json=list_json.get(1);
			JSONArray bets = json.getJSONArray("bets");
			bet = bets.getLongValue(bet_index);
		}

		msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_bet.subCode_value);
		msg.put(Protocols.G2c_niuniu_bet.ID, id);
		//由于一局只能对一个选项押注，所以需要判断之前有没有进行过下注
		BetInfo bet_exist = bets_player.get(player.getUuid());
		if(bet_exist==null){
			BetInfo info=new BetInfo(player,id,bet);
			acceptBet(player, info);
			PlayerDataService.getInstance().modifyCoin(player, -bet,EventLogType.niuniu_bet);
			msg.put(Protocols.G2c_niuniu_bet.BETS, bet);
			PlayerMsgSender.getInstance().addMsg(msg);
			
		}else{
			if(bet_exist.id!=id){
				msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str29"));
				PlayerMsgSender.getInstance().addMsg(msg);
				return;
			}
			long total=bet_exist.bet+bet;
			if(total>(player.getCoin()-bet)/4){
				msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str30"));
				PlayerMsgSender.getInstance().addMsg(msg);
				return;
			}
			PlayerDataService.getInstance().modifyCoin(player, -bet,EventLogType.niuniu_bet);
			msg.put(Protocols.G2c_niuniu_bet.BETS, total);
			PlayerMsgSender.getInstance().addMsg(msg);
			bet_exist.bet=total;
			acceptBet(player, bet_exist);
		}

		ClientMsg msg_8 = new ClientMsg();
		msg_8.put(Protocols.MAINCODE, Protocols.G2c_niuniu_player_bet_notify.mainCode_value);
		msg_8.put(Protocols.SUBCODE, Protocols.G2c_niuniu_player_bet_notify.subCode_value);
		msg_8.put(Protocols.G2c_niuniu_player_bet_notify.UUID, player.getUuid());
		msg_8.put(Protocols.G2c_niuniu_player_bet_notify.BET, bet);
		msg_8.put(Protocols.G2c_niuniu_player_bet_notify.ID, id);
		sendTableMsg(msg_8);
		
	}
	
	
	@Override
	public void gameStart() {
		log.info("牌局开始----------------------");
		super.gameStart();
		bets_player.clear();
		bets_table.clear();
		poker_table.clear();
		bets_table.put(TIAN, new ConcurrentHashMap<String, BetInfo>());
		bets_table.put(DI, new ConcurrentHashMap<String, BetInfo>());
		bets_table.put(XUAN, new ConcurrentHashMap<String, BetInfo>());
		bets_table.put(HUANG, new ConcurrentHashMap<String, BetInfo>());
		
		refreshPlayerList();
		json_bets = new JSONObject();
		json_bets.put(Protocols.MAINCODE, Protocols.G2c_niuniu_bet_refresh.mainCode_value);
		json_bets.put(Protocols.SUBCODE, Protocols.G2c_niuniu_bet_refresh.subCode_value);
		
		ClientMsg cMsg=new ClientMsg();
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_niuniu_begin_bet.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_begin_bet.subCode_value);
		cMsg.put(Protocols.G2c_niuniu_begin_bet.REMAIN_TIMESTAMP, time_bet);
		sendTableMsg(cMsg);
		
		
		DelayMsg msg = new DelayMsg(time_bet){
			@Override
			public void onTimeUp() {
				ClientMsg cMsg = new ClientMsg();
				cMsg.setMainCode(Protocols.Inner_game_niuniu_checkout.mainCode_value);
				cMsg.put(Protocols.MAINCODE, Protocols.Inner_game_niuniu_checkout.mainCode_value);
				cMsg.put(Protocols.SUBCODE, Protocols.Inner_game_niuniu_checkout.subCode_value);
				cMsg.put(Protocols.Inner_game_niuniu_checkout.TABLEID, tableId);
				cMsg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(cMsg);
			}
		};
		InnerDelayManager.getInstance().addDelayItem(msg);
		next_event_time = System.currentTimeMillis()+time_bet;
		
	}
	
	@Override
	public void gameEnd() {
		log.info("牌局结束=====================");
		super.gameEnd();
		if(players.size()<1){
			//如果桌子上没有玩家了就回收桌子
			room.removeTable(tableId);
			return;
		}
		
		DelayMsg msg = new DelayMsg(time_wait){
			@Override
			public void onTimeUp() {
				ClientMsg cMsg = new ClientMsg();
				cMsg.setMainCode(Protocols.Inner_game_niuniu_start_bet.mainCode_value);
				cMsg.put(Protocols.MAINCODE, Protocols.Inner_game_niuniu_start_bet.mainCode_value);
				cMsg.put(Protocols.SUBCODE, Protocols.Inner_game_niuniu_start_bet.subCode_value);
				cMsg.put(Protocols.Inner_game_niuniu_start_bet.TABLEID, tableId);
				cMsg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(cMsg);
			}
		};
		InnerDelayManager.getInstance().addDelayItem(msg);
		next_event_time = System.currentTimeMillis()+time_wait;
		
		if(banker == banker_system){
			if(list_req_banker.size() > 0){
				refreshBanker();
			}
		}else{
			if(!checkBankerCondition(banker)){
				refreshBanker();
			}
		}
		//如果玩家钱输掉了，不够资格在当前牌桌，就要踢走 
		for(Player player:players.values()){
			JSONObject j1 = list_json.get(0);
			JSONObject j2 = list_json.get(1);
			if(player.getCoin()<j1.getLongValue("min")){
				playerLeave(player.getUuid());
				ClientMsg msg_kick=new ClientMsg();
				msg_kick.put(Protocols.MAINCODE, Protocols.G2c_niuniu_kick_table.mainCode_value);
				msg_kick.put(Protocols.SUBCODE, Protocols.G2c_niuniu_kick_table.subCode_value);
				msg_kick.put(Protocols.G2c_niuniu_kick_table.INFO, AllTemplate.getGameString("str31"));
				msg_kick.setChannel(player.getChannel());
				PlayerMsgSender.getInstance().addMsg(msg_kick);
			}
//			if(player.getCoin()>j2.getLongValue("max")){
//				playerLeave(player.getUuid());
//				ClientMsg msg_kick=new ClientMsg();
//				msg_kick.put(Protocols.MAINCODE, Protocols.G2c_niuniu_kick_table.mainCode_value);
//				msg_kick.put(Protocols.SUBCODE, Protocols.G2c_niuniu_kick_table.subCode_value);
//				msg_kick.put(Protocols.G2c_niuniu_kick_table.INFO, "钱太多了，换个场子吧");
//				msg_kick.setChannel(player.getChannel());
//				PlayerMsgSender.getInstance().addMsg(msg_kick);
//			}
		}
	}
	
	/**
	 * 接受下注
	 * @param player
	 * @param betInfo
	 */
	private void acceptBet(Player player,BetInfo betInfo){
		bets_player.put(player.getUuid(), betInfo);
		ConcurrentHashMap<String, BetInfo> map = bets_table.get(betInfo.id);
		map.put(player.getUuid(), betInfo);
		
		//通知桌子上的玩家筹码有刷新
		json_bets.put(Protocols.MAINCODE, Protocols.G2c_niuniu_bet_refresh.mainCode_value);
		json_bets.put(Protocols.SUBCODE, Protocols.G2c_niuniu_bet_refresh.subCode_value);
		JSONObject[] betArray=new JSONObject[bets_table.size()];
		int i=0;
		for(Integer id:bets_table.keySet()){
			ConcurrentHashMap<String, BetInfo> _map =bets_table.get(id);
			long bets = 0;
			for(BetInfo info:_map.values()){
				bets += info.bet;
			}
			JSONObject json=new JSONObject();
			json.put(Protocols.G2c_niuniu_bet_refresh.BetInfo.ID, id);
			json.put(Protocols.G2c_niuniu_bet_refresh.BetInfo.BETS, bets);
			betArray[i]=json;
			i++;
		}
		json_bets.put(Protocols.G2c_niuniu_bet_refresh.BETINFO, betArray);
		ClientMsg msg = new ClientMsg();
		msg.setJson(JsonUtil.copyJson(json_bets));
		sendTableMsg(msg);
		
	}
	
	public void checkout(){
		dealCards();
		sendSystemMsg();
		sendResult();
		settlement();
		addExp();
		gameEnd();
	}
	
	private void addExp() {
		for(String uuid:bets_player.keySet()){
			Player player = players.get(uuid);
			if(player!=null){
				PlayerDataService.getInstance().addExp(player, exp);
			}
		}
		if(banker!=banker_system){
			PlayerDataService.getInstance().addExp(banker, exp);
		}
	}

	private void sendResult() {
		ClientMsg msg = new ClientMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2c_niuniu_result.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_result.subCode_value);
		msg.put(Protocols.G2c_niuniu_result.RESULTINFO, resultInfo);
		msg.put(Protocols.G2c_niuniu_result.REMAIN_TIMESTAMP, time_wait);
		sendTableMsg(msg);
		
		if(banker!=banker_system){
			ClientMsg msg_bank_coin=new ClientMsg();
			msg_bank_coin.put(Protocols.MAINCODE, Protocols.G2c_niuniu_banker_coin_refresh.mainCode_value);
			msg_bank_coin.put(Protocols.SUBCODE, Protocols.G2c_niuniu_banker_coin_refresh.subCode_value);
			msg_bank_coin.put(Protocols.G2c_niuniu_banker_coin_refresh.COIN, banker.getCoin());
			sendTableMsg(msg_bank_coin);
		}
	}

	/**
	 * 发牌
	 */
	private void dealCards() {
		// 先洗出5副牌
		poker_table.clear();
		int[] cards_all = PokerCommon.initCardsExceptJoker();
//		Utils.printArray(cards_all, 52);
		PokerCommon.shuffle(cards_all);
//		Utils.printArray(cards_all, 52);
//		int hasSame = Test2.hasSame(cards_all);
//		if(hasSame>=0){
//			System.out.println("hasSame="+hasSame);
//		}
		for (int i = 0; i < 5; i++) {
			int cards[] = new int[5];
			System.arraycopy(cards_all, i * 5, cards, 0, 5);
			NiuniuPoker poker = new NiuniuPoker(cards);
			poker_table.add(poker);
		}
		// 将集合排序，大的牌放在前面
		Collections.sort(poker_table, NiuniuPoker.comparator);

		// 把五副牌发到桌上
		int[] poker_index = getPokerIndex();
		for (int i = 0; i < poker_index.length; i++) {
			NiuniuPoker poker = poker_table.get(i);
			poker.index_rank = i;
			poker.index_table_poker = poker_index[i];
			if (poker_index[i] == 0)
				poker_banker = poker;
			System.out.println(poker.toString());
		}
		resultInfo=new JSONObject[5];
		for (int i = 0; i < poker_table.size(); i++) {
			NiuniuPoker poker = poker_table.get(i);
			resultInfo[i] = poker.toJsonObject();
		}
	}
	
	private int[] getPokerIndex(){
		if(banker != banker_system){
			int[] poker_index={0,1,2,3,4};
			PokerCommon.shuffle(poker_index);
			return poker_index;
		}else{//   如果是系统坐庄还要换一种算法
			return getSystemBankerPokerIndex();
		}
	}
	
	private void sendSystemMsg(){
		NiuniuPoker poker_biggest = poker_table.get(0);
		if(banker == banker_system && poker_biggest.index_table_poker == 0){ //如果是系统坐庄，只有当“天、地、玄、黄”任意一位置开出“五花牛”和“五小牛”牌型时
			return;
		}
		if(poker_biggest.cardType == GameCardType.DN_NN_LESS_10 || poker_biggest.cardType == GameCardType.DN_ALL_ROYAL){
			
			ConcurrentHashMap<String, BetInfo> bets = bets_table.get(poker_biggest.index_table_poker);
			StringBuffer sb = new StringBuffer("");
			if(bets.size()==0 && poker_biggest.index_table_poker != 0){ //没人下注
				sb.append(AllTemplate.getGameString("str32"));
				if(lvl==1)
					sb.append(AllTemplate.getGameString("str33"));
				else if (lvl==2) 
					sb.append(AllTemplate.getGameString("str34"));
				else
					sb.append(AllTemplate.getGameString("str35"));
				sb.append(AllTemplate.getGameString("str36"));
				sb.append(poker_biggest.cardType.getDesc());
				sb.append(AllTemplate.getGameString("str37"));
			}
			else {
				sb.append(AllTemplate.getGameString("str38"));
				if (lvl == 1)
					sb.append(AllTemplate.getGameString("str33"));
				else if (lvl == 2)
					sb.append(AllTemplate.getGameString("str34"));
				else
					sb.append(AllTemplate.getGameString("str35"));
				sb.append(AllTemplate.getGameString("str39"));
				sb.append(poker_biggest.cardType.getDesc());
				sb.append(AllTemplate.getGameString("str40"));
				long reward = 0l;
				if (poker_biggest.index_table_poker == 0) { // 庄家赢钱
					for (ConcurrentHashMap<String, BetInfo> bet : bets_table.values()) {
						for (BetInfo betInfo : bet.values()) {
							reward = reward + (betInfo.bet * poker_biggest.cardType.getOdds());
						}
					}
				} else {
					for (BetInfo betInfo : bets.values()) {
						reward = reward + (betInfo.bet * poker_biggest.cardType.getOdds());
					}
				}
				sb.append(reward).append(AllTemplate.getGameString("str41"));
			}
			
			ChatService.getInstance().sendGameMsg(sb.toString());
		}
	}
	
	private int[] getSystemBankerPokerIndex(){
		int poker_banker_index=0; //庄家拿第几副牌
		JSONArray array_probobality=null;
		if(!pool.canOpenWater()){ //吃分期
			array_probobality =AllTemplate.getNiuniu_probobality1();
		}else{
			array_probobality =AllTemplate.getNiuniu_probobality2();
		}
		int value = new Random().nextInt(100);
		for (int i = 0; i < array_probobality.size(); i++) {
			JSONObject jb = array_probobality.getJSONObject(i);
			if(value >= jb.getIntValue("startValue") && value<=jb.getIntValue("endValue")){
				poker_banker_index = jb.getIntValue("index");
			}
		}

//		poker_banker_index=2; //庄家拿第几副牌
		
		
		int[] poker_index=new int[5];
		int[] index_player_poker={1,2,3,4};
		PokerCommon.shuffle(index_player_poker);
		System.arraycopy(index_player_poker, 0, poker_index, 0, poker_banker_index);
		poker_index[poker_banker_index]=0;
		System.arraycopy(index_player_poker, poker_banker_index, poker_index, poker_banker_index+1, 4-poker_banker_index);
		System.out.println("排序");
		Utils.printArray(poker_index, 5);
		
//		poker_index = Utils.getRandomIntArrayDistinct(0, 5, 5);
//		PokerCommon.shuffle(poker_index);
		
		return poker_index;
	}
	
	/**
	 * 结算
	 */
	private void settlement() {
		long reward_banker = 0;

		
		ArrayList<JSONObject> player8_rewardList=new ArrayList<>();
		// 拿闲家的牌和庄家的牌比大小
		for (int i = 0; i < poker_table.size(); i++) {
			NiuniuPoker poker = poker_table.get(i);
			if (poker.index_table_poker == 0)
				continue;
			// 闲赢
			ConcurrentHashMap<String, BetInfo> playerBets = bets_table.get(poker.index_table_poker);
			if (poker.index_rank < poker_banker.index_rank) {
				int odds = poker.cardType.getOdds();
				for (BetInfo bet : playerBets.values()) {
					Player player_xian = bet.player;
					long reward_player = (long) (bet.bet + bet.bet * odds * rate_commission);
					pool.addWater((long)(bet.bet * odds * (1-rate_commission)));
					long coin_lose=bet.bet * odds;
					reward_banker = reward_banker - coin_lose;
					if(banker == banker_system){
						pool.reduceWater(coin_lose);
					}
					PlayerDataService.getInstance().modifyCoin(player_xian, reward_player,EventLogType.niuniu_checkout);
					if(players_coin_rank.get(player_xian.getUuid())!=null){ //如果玩家是在前八
						JSONObject j = new JSONObject();
						j.put(Protocols.G2c_niuniu_8reward.Player_reward.UUID, player_xian.getUuid());
						j.put(Protocols.G2c_niuniu_8reward.Player_reward.REWARD, reward_player);
						player8_rewardList.add(j);
					}
				}
			}
			// 庄赢
			else {
				int odds = poker_banker.cardType.getOdds();
				for (BetInfo bet : playerBets.values()) {
					Player player_xian = bet.player;
					long reward_player = (long) (bet.bet * (odds -1)* -1);
					long show_reward_player = (long) (bet.bet * odds* -1);
					reward_banker = reward_banker + (long)(bet.bet * odds * rate_commission);
					pool.addWater((long)(bet.bet * odds * (1-rate_commission)));
					PlayerDataService.getInstance().modifyCoin(player_xian, reward_player,EventLogType.niuniu_checkout);
					if(players_coin_rank.get(player_xian.getUuid())!=null){ //如果玩家是在前八
						JSONObject j = new JSONObject();
						j.put(Protocols.G2c_niuniu_8reward.Player_reward.UUID, player_xian.getUuid());
						j.put(Protocols.G2c_niuniu_8reward.Player_reward.REWARD, show_reward_player);
						player8_rewardList.add(j);
					}
				}
			}
		}

		if(banker != banker_system){
			PlayerDataService.getInstance().modifyCoin(banker, reward_banker,EventLogType.niuniu_checkout);
		}
		
		JSONObject[] jsons=new JSONObject[player8_rewardList.size()];
		for (int i = 0; i < jsons.length; i++) {
			jsons[i]= player8_rewardList.get(i);
		}
		ClientMsg msg_reward8=new ClientMsg();
		msg_reward8.put(Protocols.MAINCODE, Protocols.G2c_niuniu_8reward.mainCode_value);
		msg_reward8.put(Protocols.SUBCODE, Protocols.G2c_niuniu_8reward.subCode_value);
		msg_reward8.put(Protocols.G2c_niuniu_8reward.PLAYER_REWARD , jsons);
		sendTableMsg(msg_reward8);
	}

	private void refreshBanker() {
		if (players.size() <= 1 || list_req_banker.size() < 1) {
			// 只有一个玩家,或者没有申请上庄的人
			banker = banker_system;
		} else {
			Player player = players.get(list_req_banker.remove(0));
			// 看请求上庄的玩家是否符合条件
			if (!checkBankerCondition(player)) {
				refreshBanker();
			} else {
				banker = player;
			}
		}

		refreshJsonBankerList();
		sendBankerInfo();
		
//		// 通知在上庄队列中的玩家更新序列
//		int index=1;
//		for(String uuid:list_req_banker){
//			Player player = players.get(uuid);
//			if(player!=null)
//			{
//				ClientMsg msg = new ClientMsg();
//				msg.put(Protocols.MAINCODE, Protocols.G2c_niuniu_banker_index.mainCode_value);
//				msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_banker_index.subCode_value);
//				msg.put(Protocols.G2c_niuniu_banker_index.INDEX, index);
//				msg.setChannel(player.getChannel());
//				PlayerMsgSender.getInstance().addMsg(msg);
//				index++;
//			}
//		}
	}
	
	private void refreshJsonBankerList(){
		json_banker_info = new JSONObject();
		json_banker_info.put(Protocols.MAINCODE, Protocols.G2c_niuniu_banker_list.mainCode_value);
		json_banker_info.put(Protocols.SUBCODE, Protocols.G2c_niuniu_banker_list.subCode_value);
		if (list_req_banker.size() > 0) {
			String[] list = new String[list_req_banker.size()];
			for (int i = 0; i < list.length; i++) {
				list[i] = list_req_banker.get(i);
			}
			json_banker_info.put(Protocols.G2c_niuniu_banker_list.LIST, list);
		}

		JSONObject json_banker = new JSONObject();
		json_banker.put(Protocols.G2c_niuniu_banker_list.Banker.UUID, banker.getUuid());
		json_banker.put(Protocols.G2c_niuniu_banker_list.Banker.NAME, banker.getName());
		json_banker.put(Protocols.G2c_niuniu_banker_list.Banker.COIN, banker.getCoin());
		json_banker.put(Protocols.G2c_niuniu_banker_list.Banker.HEAD, banker.getHead());
		if(banker == banker_system){
			json_banker.put(Protocols.G2c_niuniu_banker_list.Banker.SYSTEM, true);
		}else
			json_banker.put(Protocols.G2c_niuniu_banker_list.Banker.SYSTEM, false);
		json_banker_info.put(Protocols.G2c_niuniu_banker_list.BANKER, json_banker);
	}
	
	/**
	 * 看玩家条件够不够上庄
	 * @param player
	 * @return
	 */
	private boolean checkBankerCondition(Player player){
		long condition = list_json.get(0).getLongValue("banker_condition");
		if (player == null || !player.isOnline() || player.getCoin() < condition) {
			return false;
		}
		return true;
	}
	
	private void sendBankerInfo(){
		ClientMsg msg = new ClientMsg();
		msg.setJson(JsonUtil.copyJson(json_banker_info));
		sendTableMsg(msg);
	}
	
	public void reqBanker(Player player,ClientMsg msg){
		long condition = list_json.get(0).getLongValue("banker_condition");
		if(player.getCoin()<condition){
			msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_req_banker.subCode_value);
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str42"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		if(list_req_banker.contains(player.getUuid())){
			msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_req_banker.subCode_value);
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str43"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		if(players.size()<=1){
			msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_req_banker.subCode_value);
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str44"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		if(player == banker){
			msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_req_banker.subCode_value);
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str45"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_req_banker.subCode_value);
		msg.put(Protocols.G2c_niuniu_req_banker.INDEX, list_req_banker.size());
		PlayerMsgSender.getInstance().addMsg(msg);
		list_req_banker.addLast(player.getUuid());
		refreshJsonBankerList();
		sendBankerInfo();
		if(list_req_banker.size()==1 && table_status==STATUS_WAITING)
			refreshBanker();
	}
	
	public void quitBanker(Player player,ClientMsg msg){
		msg.put(Protocols.SUBCODE, Protocols.G2c_niuniu_quit_banker.subCode_value);
		if(this.table_status == STATUS_WAITING){
			refreshBanker();
		}else
		{
			msg.put(Protocols.ERRORCODE, "牌局已经开始，无法下庄");
		}
		PlayerMsgSender.getInstance().addMsg(msg);
	}
	
	
	@Override
	public void playerLeave(String uuid) {
		Player player =players.get(uuid);
		if(player!=null)
		{
			if(player == banker)
				refreshBanker();
		}
		super.playerLeave(uuid);
		
	}
	
	private void refreshPlayerList() {
		players_coin_rank.clear();
		ArrayList<JSONObject> list_json = new ArrayList<>();
		ArrayList<Player> list_player = new ArrayList<>();
		for (Player player : players.values()) {
			list_player.add(player);
		}
		Collections.sort(list_player, new PlayerComparator());
		int count = 0;
		for (int i = 0; i < list_player.size(); i++, count++) {
			if (count > 7)
				break;
			Player player = list_player.get(i);
			players_coin_rank.put(player.getUuid(), player);
			JSONObject json = new JSONObject();
			json.put(Protocols.G2c_niuniu_playerlist.Players.UUID, player.getUuid());
			json.put(Protocols.G2c_niuniu_playerlist.Players.NAME, player.getName());
			json.put(Protocols.G2c_niuniu_playerlist.Players.COIN, player.getCoin());
			json.put(Protocols.G2c_niuniu_playerlist.Players.HEAD, player.getHead());
			list_json.add(json);
		}

		JSONObject[] json_rank8_players = new JSONObject[list_json.size()];
		for (int i = 0; i < json_rank8_players.length; i++) {
			json_rank8_players[i] = list_json.get(i);
		}
		json_player_list.put(Protocols.G2c_niuniu_playerlist.PLAYERS, json_rank8_players);
		json_player_list.put(Protocols.MAINCODE, Protocols.G2c_niuniu_playerlist.mainCode_value);
		json_player_list.put(Protocols.SUBCODE, Protocols.G2c_niuniu_playerlist.subCode_value);
		ClientMsg msg = new ClientMsg();
		msg.setJson(JsonUtil.copyJson(json_player_list));
		sendTableMsg(msg);
	}

	@Override
	public void playerReconnect(Player player) {
		sendTableInfo(player,true);
	}

	@Override
	public void closeTable() {
		// TODO Auto-generated method stub
		
	}
	
}

class BetInfo{
	public Player player;
	public long bet;
	public int id;
	BetInfo(Player player,int id,long bet){
		this.player = player;
		this.id = id;
		this.bet = bet;
	}
}

class PlayerComparator implements Comparator<Player>{

	@Override
	public int compare(Player p1, Player p2) {
		if(p1.getCoin()>p2.getCoin())
			return -1;
		return 1;
	}
	
}


class Pool{
	private long warning_line=10000000l;

	private long water=0;
	private boolean openWater=false;
	public void addWater(long _water){
		this.water = this.water +_water;
		if(this.water>warning_line)
			openWater=true;
	}
	
	public void reduceWater(long _water){
		this.water = this.water - _water;
		if(this.water<0)
			openWater=false;
	}
	
	public boolean canOpenWater(){
		return openWater;
	}
	public long getWarning_line() {
		return warning_line;
	}

	public void setWarning_line(long warning_line) {
		this.warning_line = warning_line;
	}
}


