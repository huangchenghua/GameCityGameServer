package com.gz.gamecity.gameserver.table;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.lang.String;
import java.lang.Long;
import java.lang.Math;
import java.util.Comparator;


import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.gz.util.JsonUtil;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.delay.DelayMsg;
import com.gz.gamecity.gameserver.delay.InnerDelayManager;
import com.gz.gamecity.gameserver.room.Room;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.gameserver.service.niuniu.Const;
import com.gz.gamecity.gameserver.service.niuniu.Const.GameType;
import com.gz.gamecity.gameserver.service.texas.TexasUtil;
import com.gz.gamecity.gameserver.service.niuniu.PokerCommon;
import com.gz.gamecity.protocol.Protocols;
import com.gz.gamecity.protocol.Protocols.G2c_texas_remove_seat;
import com.gz.websocket.msg.ClientMsg;

import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;


public class TexasTable extends GameTable {
	public static enum ActionType {
		NONE			(0),		// 未行动过
		DOING			(1),		
		BLIND_BET		(10),		// 盲注
		CALL 			(11), // 跟进
		FOLD			(12),	// 弃牌
		CHECK			(13),	// 让牌
		RAISE			(14),	// 加注
		ALL_IN			(15);	// 全押
		
		private int value;
		private ActionType(int value) {
			this.value = value;
		}
		public int value() { return this.value; }
		
		public static ActionType nameOfValue(int value) {
			for (ActionType e : ActionType.values()) {
				if (e.value() == value) {
					return e;
				}
			}
			return NONE;
		}
	}
	
	public static enum PlayerStatus {
		WAITTING	(0),
		READY	(1),
		LEAVE	(2),
		STAND_UP	(3);
		//BET			(10),
		//CALL		(11),
		//FOLD		(12),
		//CHECK		(13),
		//RAISE		(14),
		//ALL_IN		(15);
				
		private int value;
		private PlayerStatus(int value) {
			this.value = value;
		}
		
		public int value() {
			return this.value;
		}
		
		public static PlayerStatus nameOfValue(int value) {
			for (PlayerStatus e : PlayerStatus.values()) {
				if (e.value() == value) {
					return e;
				}
			}
			return WAITTING;
		}
	}

	
	private static final Logger log = Logger.getLogger(TexasTable.class);
	
	private static Comparator<SeatInfo> compScore = new Comparator<SeatInfo>() {
		@Override
		public int compare(SeatInfo seat1, SeatInfo seat2) {
			if (seat1.nBet < seat2.nBet) {
				return 1;
			} 
			return -1;
		}
		
	};
	
	class SeatInfo{
		public Player player;
		public int nSeatIndex;
		public long nBet;
		public PlayerStatus eStatus;
		public int nCard1;
		public int nCard2;
		public boolean bShowCard;
		//public boolean bIsCheck;
		public ActionType eAction;
		public int nScore;
		
		SeatInfo(Player player, int nSeatIndex){
			this.nSeatIndex = nSeatIndex;
			
			init(player);
		}
		
		void init(Player player) {
			this.player = player;
			this.nBet = 0;
			this.eStatus = PlayerStatus.WAITTING;
			this.nCard1 = -1;
			this.nCard2 = -1;
			this.bShowCard = false;
			this.eAction = ActionType.NONE;
			this.nScore = 0;
		}
		
		
		void clear() {
			init(null);
		}
		
		boolean canReward() {// 
			if (this.nBet == 0) 
				return false;
			
			if (this.eAction == ActionType.NONE || this.eAction == ActionType.FOLD)
				return false;
			
			return true;
		}
		
		boolean canAction() {//可以继续下注的玩家
			if (this.eStatus == PlayerStatus.WAITTING || this.eStatus == PlayerStatus.STAND_UP) {
				return false;
			}
			
			if (this.eAction == ActionType.FOLD || this.eAction == ActionType.ALL_IN) {
				return false;
			}
			
			return true;
		}
	}
	
	class PotInfo {
		public ArrayList<SeatInfo> arraySeat;
		public long nPotValue;
		public long nBetLimit;
		
		public PotInfo(long nBetLimit) {
			this.arraySeat = new ArrayList<SeatInfo>();
			
			this.nBetLimit = nBetLimit;
			this.nPotValue = 0;
		}
		
		public PotInfo(PotInfo potInfo) {
			this.arraySeat = new ArrayList<SeatInfo>(potInfo.arraySeat);
			
			this.nBetLimit = potInfo.nBetLimit;
			this.nPotValue = 0;
		}
		
		// add player who can get reward 
		public void tryAddPlayer(SeatInfo seatInfo) {
			if (seatInfo.player == null)
				return;
			
			if ( (seatInfo.eStatus == PlayerStatus.WAITTING) || (seatInfo.eStatus == PlayerStatus.STAND_UP) )
				return;
			if (seatInfo.eAction == ActionType.FOLD)
				return;
			if (seatInfo.eAction == ActionType.ALL_IN && seatInfo.nBet < nBetLimit)
				return;
			
			if(arraySeat.isEmpty()) {
				arraySeat.add(seatInfo);
			} else {
				if (arraySeat.get(0).nScore < seatInfo.nScore) {
					arraySeat.clear();
					arraySeat.add(seatInfo);
				} else if (arraySeat.get(0).nScore == seatInfo.nScore) {
					arraySeat.add(seatInfo);
				}
			}
			log.debug("texas pot add player[uuid=" + seatInfo.player.getUuid() + " bet_limit=" + this.nBetLimit + " score=" + seatInfo.nScore + "]");
		}
		
		public int getScore() {
			if (arraySeat.isEmpty())
				return 0;
			return arraySeat.get(0).nScore;
		}
		
	}
	
	class Winner {
		public SeatInfo seatInfo;
		public long nCoinWin;
		public int nScore;
		
		public Winner(SeatInfo seatInfo, long nCoinWin, int nScore) {
			this.seatInfo = seatInfo;
			this.nCoinWin = nCoinWin;
			this.nScore = nScore;
		}
	}
	
	private static final int N_PLAYER_MIN = 3;
	private static final int N_PLAYER_MAX = 7;
	
	private static final int N_PUBLIC_CARD_MAX = 5;
	private static final int N_HAND_CARD_MAX = 2;
	
	private static final long N_START_DELAY	= 5 * 1000l;
	private static final long N_PLAYER_ACTION_DELAY = 30 * 1000l;

	private int m_nLv;				// 等级场
	private long m_nBlindsMin;		//最小盲注
	private long m_nBlindsMax;	 // 最大盲注
	private long m_nBetMin;	 // 最小压注
	private float m_fRakeRate; // 抽成 百分比率	
	
	//private SeatInfo[] m_szSeatInfo = new SeatInfo[N_PLAYER_MAX]; // 座位信息
	private int[] m_szCardDeck;
	private ArrayList<SeatInfo> m_listSeatInfo = new ArrayList<SeatInfo>();
	
	//private LinkedList<Player> m_listWaittinPlayer = new LinkedList<Player>();
	
	private int[] m_szPublicCard = new int[N_PUBLIC_CARD_MAX];
	
	private ArrayList<PotInfo> m_arrayPot = new ArrayList<PotInfo>();
	

	/*
	 *  m_nRound :
	 *  -2 人数不足，未开始
	 *  -1 人数足够，准备开始，等待中
	 *  0 发2张手牌每人，然后轮流下注
	 *  1 发3张公共牌，然后轮流下注
	 *  2 发第4张公共牌，然后轮流下注
	 *  3 发第5张公共牌，然后轮流下注
	 *  4 全部下注结束，开始翻牌，计算牌局结果
	 * 
	 * */
	private int m_nRound; 
	
	private int m_nMatchCnt;	// 场次
	private int m_nRaiseBeginIndex;	// 加注的开始位置
	private int m_nActionIndex;	// 正在行动的玩家位置
	private long m_nBetNow;		// 目前桌面上压注的金额
	private int m_nButtonIndex;
	private int m_nPotTotal;		// 桌面所有玩家押下的总金额
	
	
	private String m_strMsgId;
	
	private boolean m_bCanCheck;	// 当前回合是否是让牌状态

	public TexasTable(Room room) {
		super(room);
		
		m_nRound = -2;
		m_nMatchCnt = 0;
		m_nRaiseBeginIndex = -1;
		m_nActionIndex = -1;
		m_nBetNow = 0;
		m_nButtonIndex = 0;
		
		m_listSeatInfo.clear();
		for (int i = 0; i < N_PLAYER_MAX; ++i) {
			SeatInfo seatInfo = new SeatInfo(null, i);
			m_listSeatInfo.add(seatInfo);
		}
	}


	@Override
	public void playerReconnect(Player player) {
		// TODO Auto-generated method stub
		SeatInfo seatInfo = findSeatByPlayer(player.getUuid());
		if (seatInfo == null)
			return;
		if (super.table_status == STATUS_ONGOING) {
			//seatInfo.eStatus = PlayerStatus.READY;
			updatePlayerStatus(seatInfo, PlayerStatus.READY);
		} else {
			//seatInfo.eStatus = PlayerStatus.WAITTING;
			updatePlayerStatus(seatInfo, PlayerStatus.WAITTING);
		}
		
		log.debug("texas player reconnect[" + player.getUuid() +"]");
		
		
		ClientMsg retMsg = new ClientMsg();
		retMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_choose_lv.mainCode_value);
		retMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_choose_lv.subCode_value);
		
		retMsg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(retMsg);
		
		
		ClientMsg clientMsg = pkgTableInfoMsg();
		clientMsg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(clientMsg);

		sendHandCard(player);
		
		sendTableMsg(pkgSeatInfoMsg(seatInfo));
		
	}
	
	@Override
	public void playerOffline(String uuid) {
		// TODO Auto-generated method stub
		
		if(super.table_status == STATUS_WAITING) {
			removePlayer(uuid);
		} else {
			SeatInfo seatInfo = findSeatByPlayer(uuid);
			if (seatInfo != null) {
			//seatInfo.eStatus = PlayerStatus.LEAVE;
				updatePlayerStatus(seatInfo, PlayerStatus.LEAVE);
			}
		}
		
		log.debug("texas palyer offline[uuid=" + uuid + " table_status=" + super.table_status);
	}

	@Override
	public boolean canLeave(String uuid) {
		// TODO Auto-generated method stub
		return false;
		/*log.debug("texas can leave [uuid=" + uuid + "]");
		if(super.table_status == STATUS_WAITING)
			return true;
		
		SeatInfo seatInfo = findSeatByPlayer(uuid);
		if (seatInfo != null)
			updatePlayerStatus(seatInfo, PlayerStatus.LEAVE);
		return false;
		*/
	}
	
	@Override
	public boolean playerSitDown(Player player) {
		if(super.playerSitDown(player) == false) {
			return false;
		}
		
		if (hasInSeat(player) == true) {
			log.error("player has already in seat[uuid=" + player.getUuid() + "]");
			return false;
		}
		
		SeatInfo seatInfo = findEmptySeat();
		if (seatInfo == null) {
			log.error("seat is full[uuid=" + player.getUuid() + "]");
			return false;
		}
		
		seatInfo.init(player);
			
		ClientMsg clientMsg = pkgTableInfoMsg();
		clientMsg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(clientMsg);

		sendHandCard(player);
		
		ClientMsg tmpMsg = pkgSeatInfoMsg(seatInfo);
		sendTableMsg(tmpMsg);

		log.debug("player enter table[uuid=" + player.getUuid() + " seatIndex=" + seatInfo.nSeatIndex + "]");

		tryRunRound();
			
		return true;

	}
	
	@Override
	public void playerLeave(String uuid){
		
		/*
		log.debug("texas player leave[uuid=" + uuid + "]");
		removePlayer(uuid);
		*/
	}
	
	public int getPlayerCntCanAction() {
		int nCnt = 0;
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if(seatInfo.player == null)
				continue;
			if(seatInfo.canAction() == false)
				continue;
			
			++nCnt;
		}
		return nCnt;
	}
	
	public void removePlayer(String strUuid) {
		super.playerLeave(strUuid);
		
		SeatInfo seatInfo = findSeatByPlayer(strUuid);
		if (seatInfo == null || seatInfo.player == null) {
			log.error("not find player in seat[uuid=" + strUuid);
			return ;
		}
		
		//seatInfo.clear();

		//ClientMsg clientMsg = pkgSeatInfoMsg(seatInfo);
		//sendTableMsg(clientMsg);
		
		removeSeatInfo(seatInfo);
		
		log.debug("player leave texas table,clean [uuid=" + strUuid);
	}
	
	public void setJSONData(JSONObject jo) {
		m_nLv = jo.getIntValue("lv");
		m_nBlindsMin = jo.getLongValue("blinds_min");
		m_nBlindsMax = jo.getLongValue("blinds_max");
		m_nBetMin = jo.getLongValue("bet_min");
		m_fRakeRate = jo.getFloatValue("rate_commission");
	}
	
	/*public long getTotalPotValue() {
		long nTotal = 0;
		for (int i = 0; i < m_arrayPot.size(); ++i) {
			nTotal += m_arrayPot.get(i).nPotValue;
		}
		return nTotal;
	}*/
	
	public int calcPlayerScore(SeatInfo seatInfo) {
		return calcPlayer(seatInfo, 5);
		/*if (seatInfo.player == null || seatInfo.nCard1 == -1 || seatInfo.nCard2 == -1) {
			log.error("error seat data to calc score[seat_index=" + seatInfo.nSeatIndex);
			return 0;
		}
		
		int nMaxScore = 0;
		int[] szCard = new int[5];
		szCard[0] = seatInfo.nCard1;
		szCard[1] = seatInfo.nCard2;
		for (int i = 0; i < m_szPublicCard.length; ++i) {
			for (int j = 0; j < i; ++j) {
				for (int k = 0; k < j; ++k) {
					szCard[2] = m_szPublicCard[i];
					szCard[3] = m_szPublicCard[j];
					szCard[4] = m_szPublicCard[k];
					int nTmp = TexasUtil.calcCardScore(szCard);
					//log.debug("calc score[uuid=" + seatInfo.player.getUuid() + " score=" + nTmp + " card=" + szCard[0] +", " + szCard[1] + ", " + szCard[2] + ", " + szCard[3] + ", " + szCard[4] + " ]");
					nMaxScore = Math.max(nMaxScore, nTmp);
 				}
			}
		}
		/*
		 *  1 2 3 4 5
		 *  * * *
		 *  * *   *
		 *  *   * *
		 *    * * *
		 *  * *     *
		 *  *   *   *
		 *    * *   *
		 *  *     * *
		 *    *   * *
		 *      * * *  
		 *	
		 * */
		//log.debug("texas player max score[uuid=" + seatInfo.player.getUuid() + " score_max=" + nMaxScore);
		//return nMaxScore;
		
	}
	
	public int calcPlayer(SeatInfo seatInfo, int nNum) {
		if (seatInfo.player == null || seatInfo.nCard1 == -1 || seatInfo.nCard2 == -1) {
			log.error("error seat data to calc score[seat_index=" + seatInfo.nSeatIndex + "]");
			return 0;
		}
		
		if (nNum > m_szPublicCard.length)
			nNum = m_szPublicCard.length;
		
		int nMaxScore = 0;
		int[] szCard = new int[5];
		szCard[0] = seatInfo.nCard1;
		szCard[1] = seatInfo.nCard2;
		
		for (int i = 0; i < nNum; ++i) {
			for (int j = 0; j < i; ++j) {
				for (int k = 0; k < j; ++k) {
					szCard[2] = m_szPublicCard[i];
					szCard[3] = m_szPublicCard[j];
					szCard[4] = m_szPublicCard[k];
					int nTmp = TexasUtil.calcCardScore(szCard, 2);
					//log.debug("calc score[uuid=" + seatInfo.player.getUuid() + " score=" + nTmp + " card=" + szCard[0] +", " + szCard[1] + ", " + szCard[2] + ", " + szCard[3] + ", " + szCard[4] + " ]");
					nMaxScore = Math.max(nMaxScore, nTmp);
 				}
			}
		}
		/*
		 *  1 2 3 4 5
		 *  * * *
		 *  * *   *
		 *  *   * *
		 *    * * *
		 *  * *     *
		 *  *   *   *
		 *    * *   *
		 *  *     * *
		 *    *   * *
		 *      * * *  
		 *	
		 * */
		log.debug("texas player max score[uuid=" + seatInfo.player.getUuid() + " score_max=" + nMaxScore);
		return nMaxScore;
		
	}
	
	
	public int getLv() { return m_nLv; }
	
	public boolean isFull() {
		return players.size() >= N_PLAYER_MAX;
	}
		
	public SeatInfo findSeatByPlayer(String strUuid) {
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player != null && seatInfo.player.getUuid() == strUuid) {
				return seatInfo;
			}
		}
		return null;
	}
	
	public SeatInfo findSeatNextActionThisRound() {
		return findSeatCanAction(m_nActionIndex + 1, m_nRaiseBeginIndex);
	}
	
	public SeatInfo findSeatCanAction(int nBeginIndex, int nEndIndex) {
		// [nBeginIndex, nEndIndex)
		
		SeatInfo seatInfo = null;

		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			int nNowIndex = (nBeginIndex + i) % m_listSeatInfo.size();
			if (nNowIndex == nEndIndex)
				break;
			
			SeatInfo tmp = m_listSeatInfo.get(nNowIndex);
			if ( tmp.player != null && tmp.canAction()) {
				seatInfo = tmp;
				break;
			}
		}
		return seatInfo;
	}
	
	public SeatInfo findSeatNextAction() {
		return findSeatNextActionByIndex(m_nActionIndex);
	}
	
	public SeatInfo findSeatFirstHand() { // 每回合第一个可以出手的人
		return findSeatCanAction(getButtonIndex() + 1, getButtonIndex());
	}
	
	// nIndex 索引后面，下一次可以出牌的人
	public SeatInfo findSeatNextActionByIndex(int nIndex) {
		SeatInfo seatInfo = null;

		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo tmp = m_listSeatInfo.get((nIndex + i + 1) % m_listSeatInfo.size());
			if ( tmp.player != null && tmp.canAction()) {
				seatInfo = tmp;
				break;
			}
		}
		return seatInfo;
	}

	
	/*
	public SeatInfo findSeatLastThisRound(int nIndex, PlayerStatus e) {
		SeatInfo seatInfo = null;
				
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			if (nIndex == m_nRaiseBeginIndex) // nIndex is first ,not last index existed
				break;

			int nIndexNow = (nIndex - i - 1) % m_listSeatInfo.size();
			SeatInfo tmpSeat = m_listSeatInfo.get(nIndexNow);
			if (tmpSeat.player != null && tmpSeat.eStatus == e) {
				seatInfo = tmpSeat;
				break;
			}
			if (nIndexNow == m_nRaiseBeginIndex)// begin index 
				break;

		}
		return seatInfo;
	}*/
	
	public int getButtonIndex() {
		return m_nButtonIndex;
	}
	
	public int clacButtonIndex() {
		int nIndex = 0;
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			nIndex = (m_nMatchCnt + i) % m_listSeatInfo.size();
			SeatInfo seatInfo = m_listSeatInfo.get(nIndex);
			if (seatInfo.player != null && (seatInfo.eStatus == PlayerStatus.READY || seatInfo.eStatus == PlayerStatus.LEAVE)) {
				break;
			}
		}
		return nIndex; 
	}
	
	public SeatInfo findEmptySeat() {
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			if (m_listSeatInfo.get(i).player == null) {
				return m_listSeatInfo.get(i);
			}
		}
		return null;
	}
	
	
	public boolean hasInSeat(Player player) {
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			if (m_listSeatInfo.get(i).player == player) {
				return true;
			}
		}
		return false;
	}
	
	public void removeSeatInfo(SeatInfo seatInfo) {
		seatInfo.clear();
		
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_remove_seat.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, G2c_texas_remove_seat.subCode_value);
		clientMsg.put(Protocols.G2c_texas_remove_seat.SEAT_INDEX, seatInfo.nSeatIndex);
		
		sendTableMsg(clientMsg);
	}
	
	public void playerDefaultAction() {
		Player player = null;
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null)
				continue;
			if (seatInfo.eAction == ActionType.DOING) {
				player = seatInfo.player;
				break;
			}
		}
		
		if (player == null)
			return;
		
		playerAction(player, ActionType.FOLD.value(), 0);
	}
	
	public void playerAction(Player player, int nActionType, long nBetAdd) {
		if (table_status == STATUS_WAITING) {
			log.debug("match has end[uuid=" + player.getUuid() + " action=" + nActionType + " bet_add=" + nBetAdd + "]");
			return;
		}
			
		
		
		ActionType e = ActionType.nameOfValue(nActionType);
		
		SeatInfo seatInfo = findSeatByPlayer(player.getUuid());
		if (seatInfo == null) {
			
			sendErrorMsg(player, Protocols.G2c_texas_bet.subCode_value, "玩家不在房间中");
			
			log.error("not find player in seat, uuid=" + player.getUuid());
			return;
		}
		
		if (seatInfo.eStatus != PlayerStatus.READY) {
			sendErrorMsg(player, Protocols.G2c_texas_bet.subCode_value, "不可以出牌");
			log.error("player is not ready, uuid=" + player.getUuid() + " status=" + seatInfo.eStatus.value());
			return ;
		}
		
		if (seatInfo.eAction != ActionType.DOING) {
			
			sendErrorMsg(player, Protocols.G2c_texas_bet.subCode_value, "等待其他玩家行动中");	
			log.error("player is not in doing, uuid=" + player.getUuid() + " action=" + seatInfo.eAction.value());
			return ;
		}
		
		if (e == ActionType.CALL) {
			long nCostBet = m_nBetNow - seatInfo.nBet;
			if (nCostBet < 0) {
				log.error("player error bet data[uuid=" + player.getUuid() + " BetNow=" + m_nBetNow + " betMy=" + seatInfo.nBet);
				return ;
			}
			
			if (seatInfo.player.getCoin() <= nCostBet) {
				// not enough coin call , try all in
				log.error("not enough coin, try all in[ uuid=" + seatInfo.player.getCoin() + " coin=" 
							+ seatInfo.player.getCoin() + " nBetPlayer=" + seatInfo.nBet + " nBetNow=" + m_nBetNow );
				if (seatInfo.player.getCoin() > 0) {
					playerBet(seatInfo, seatInfo.player.getCoin(), ActionType.ALL_IN);
				}
			} else {
				playerBet(seatInfo, nCostBet, ActionType.CALL);
			}
			
		} else if (e == ActionType.FOLD) {
			playerBet(seatInfo, 0, ActionType.FOLD);
		} else if (e == ActionType.CHECK) {
			if (m_bCanCheck == false) {
				sendPlayerBetInfo(player, "本回合不可以继续让牌");
				log.info("player can not check[uuid=" + player.getUuid() + " can_check=" + m_bCanCheck + "]");
				return;
			}
			playerBet(seatInfo, 0, ActionType.CHECK);
		} else if (e == ActionType.RAISE) {
			long nBetDiff = m_nBetNow - seatInfo.nBet;
			if (nBetAdd < nBetDiff) {
				sendPlayerBetInfo(player, "加注金币不足");
				log.info("player bet too little[uuid=" + player.getUuid() + " betNow=" + m_nBetNow + " betMy=" + seatInfo.nBet + "]");
				return;
			}
			
			if (player.getCoin() < nBetAdd) {
				sendPlayerBetInfo(player, "玩家金币不足");
				log.info("player not enough coin to raise [uuid=" + player.getUuid() + " coin=" + player.getCoin() + " betAdd=" + nBetAdd + " betHas=" + seatInfo.nBet);
				return ;
			}
						
			playerBet(seatInfo, nBetAdd, ActionType.RAISE);
			
		} else if (e == ActionType.ALL_IN) {
			playerBet(seatInfo, seatInfo.player.getCoin(), ActionType.ALL_IN);
		}
		
		nextPlayerDoing();
	}
	
	public void DelayStart() {
		log.debug("ready to start round[round=" + m_nRound + " left=" + N_START_DELAY + "]");
		
		DelayMsg delayMsg = new DelayMsg(N_START_DELAY) {
			@Override
			public void onTimeUp() {
				ClientMsg msg = new ClientMsg();
				msg.setMainCode(Protocols.Inner_game_texas_start.mainCode_value);
				msg.put(Protocols.MAINCODE, Protocols.Inner_game_texas_start.mainCode_value);
				msg.put(Protocols.SUBCODE, Protocols.Inner_game_texas_start.subCode_value);
				msg.put(Protocols.Inner_game_texas_start.TABLEID, tableId);
				msg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(msg);
			}
		};
		InnerDelayManager.getInstance().addDelayItem(delayMsg);
		
		// update round
		m_nRound = -1;

		// send client start time;
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.setMainCode(Protocols.G2c_texas_start_time_left.mainCode_value);
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_start_time_left.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_start_time_left.subCode_value);
		
		clientMsg.put(Protocols.G2c_texas_start_time_left.START_DELAY_SECOND, N_START_DELAY / 1000);
		
		sendTableMsg(clientMsg);
				
	}
	
	public void delayPlayerAction() {
		log.debug("texas delay to action[table_id=" + getTableId() + " seat_index=" + m_nActionIndex + " round=" + m_nRound + "]");
		
		if (m_strMsgId != null) {
			InnerDelayManager.getInstance().removeDelayItem(m_strMsgId);
			m_strMsgId = null;
		}
		
		DelayMsg delayMsg = new DelayMsg(N_PLAYER_ACTION_DELAY) {
			@Override
			public void onTimeUp() {
				
				ClientMsg clientMsg = new ClientMsg();
				clientMsg.setMainCode(Protocols.Inner_game_texas_player_action.mainCode_value);
				clientMsg.put(Protocols.MAINCODE, Protocols.Inner_game_texas_player_action.mainCode_value);
				clientMsg.put(Protocols.SUBCODE, Protocols.Inner_game_texas_player_action.subCode_value);
				clientMsg.put(Protocols.Inner_game_texas_player_action.TABLEID, getTableId());
				clientMsg.put(Protocols.Inner_game_texas_player_action.SEAT_INDEX, m_nActionIndex);
				clientMsg.put(Protocols.Inner_game_texas_player_action.ROUND, m_nRound);
				
				clientMsg.setInner(true);
				GSMsgReceiver.getInstance().addMsg(clientMsg);
				
			}
		};
		InnerDelayManager.getInstance().addDelayItem(delayMsg);
		m_strMsgId = delayMsg.getId();
	}
	
	public void tryRunRound() {
		log.debug("try run round[round=" + m_nRound + "]");
		if (m_nRound == -2  && this.getPlayerCount() >= N_PLAYER_MIN) { // 未开始 or 已经结束			
			
			startNewRound();
			
		} else if (m_nRound == 4) {
			if (this.getPlayerCount() < N_PLAYER_MIN) {
				m_nRound = -2;
				return ;
			}
			DelayStart();
		} else if (m_nRound == -1) { 	// 准备开始阶段
			if (this.getPlayerCount() < N_PLAYER_MIN) { // 人数不足，进入未开始状态等待新玩家进入
				m_nRound = -2; 
				return ;
			}
			// start round			
			startNewRound();
		}
	}
	

	public void nextPlayerDoing() {

		boolean bHasNextRound = true;
		
		if (getPlayerCntCanAction() <= 1) {
			bHasNextRound = nextRound();

		}
		
		if (bHasNextRound == false)
			return;

		
		SeatInfo seatInfo = findSeatNextActionThisRound();
		if (seatInfo == null){ // the round is end 			
			seatInfo = findSeatFirstHand();
			bHasNextRound = nextRound();
		}
		
		
		if (seatInfo == null || bHasNextRound == false) {
			return ;
		}
		seatInfo.eAction = ActionType.DOING;
		
		ClientMsg clientMsg = pkgSeatInfoMsg(seatInfo);
		sendTableMsg(clientMsg);

		// delay operate
		delayPlayerAction();
		
		log.debug("next player doing[index=" + seatInfo.nSeatIndex + " uuid=" + seatInfo.player.getUuid() + "]");
		
	}
	
	public boolean nextRound() {
		
		++m_nRound;
		updatePot();
		
		SeatInfo seatInfo = findSeatFirstHand();
		if (seatInfo != null) {
			m_nRaiseBeginIndex = seatInfo.nSeatIndex;
		}

		if (seatInfo == null || getPlayerCntCanAction() <= 1) {
			// end round
			
			log.debug("too little player can actoin, end round[round=" + m_nRound + " player_cnt_action=" + getPlayerCntCanAction() + "]");
			m_nRound = 4;
		} 

		// update check flag
		m_bCanCheck = true;
	
		// send to client
		
		// update hand card score and send hand card
		updateAndSendHandCard();
		
		// send public card
		JSONObject jo = new JSONObject();
		jo = pkgPublicTableInfo(jo);
		jo = pkgJsonPotInfo(jo);
		
		jo.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		jo.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		
		ClientMsg clienMsg = new ClientMsg();
		clienMsg.setJson(jo);
		
		sendTableMsg(clienMsg);
		
		
		if (m_nRound == 4) {
			// end round and reward
			log.debug("round end[nRound=" + m_nRound + "]");
			
			endRoundResult();
			
			return false;
		}
		
		log.debug("next round[round=" + m_nRound + " ]");

		return true;
	}
	
	public void playerBet(SeatInfo seatInfo, long nBetAdd, ActionType e) {
		
		if (seatInfo.nBet + nBetAdd > m_nBetNow) {// 本轮注码提高 并且 不是大小盲注，1 更新回合起始位置 2 让牌标志更改
			
			m_nRaiseBeginIndex = seatInfo.nSeatIndex;
			
			m_nBetNow = seatInfo.nBet + nBetAdd;
			
			// 
			m_bCanCheck = false;
			sendCheckFlagToNextAll(seatInfo.nSeatIndex);
		}
		
		// update cache data
		
		seatInfo.nBet += nBetAdd;
		seatInfo.eAction = e;
		PlayerDataService.getInstance().modifyCoin(seatInfo.player, -nBetAdd, EventLogType.texas_bet);
		
		m_nActionIndex = seatInfo.nSeatIndex;
		m_nPotTotal += nBetAdd;
		
		// send all client
		// send seat info
		ClientMsg clientMsg = pkgSeatInfoMsg(seatInfo);
		sendTableMsg(clientMsg);
		
		// send pot total
		sendPotTotalToAll();
		
		// send canCheck flag
		sendCheckFlag(seatInfo, true);

		
		log.debug("texas player bet[uuid=" + seatInfo.player.getUuid() + " bet_add=" + nBetAdd + " action=" + e.value() 
				+ " raise_begin_index=" + m_nRaiseBeginIndex + " seat_index=" + seatInfo.nSeatIndex );
	}
	
	public void startNewRound() {
		//super.gameStart();
		super.table_status = STATUS_ONGOING;
		
		//  player 
		for (Iterator<SeatInfo> it = m_listSeatInfo.iterator(); it.hasNext(); ) {
			SeatInfo seatInfo = it.next();
			if (seatInfo.player == null)
				continue;
			
			// 更新玩家当前状态
			if (seatInfo.eStatus == PlayerStatus.WAITTING) {
				//seatInfo.eStatus = PlayerStatus.READY;
				updatePlayerStatus(seatInfo, PlayerStatus.READY);
			}
		}

		// init card deck
		m_szCardDeck = PokerCommon.initCardsExceptJoker();

		PokerCommon.shuffle(m_szCardDeck);
		
		int nDrawIndex = 0;
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo tmp = m_listSeatInfo.get(i);
			if (tmp.player != null) {
				tmp.init(tmp.player);
				tmp.eStatus = PlayerStatus.READY;
				tmp.nCard1 = m_szCardDeck[nDrawIndex++];
				tmp.nCard2 = m_szCardDeck[nDrawIndex++];
				
			}
		}
		
		for (int i = 0; i < m_szPublicCard.length; ++i) {
			m_szPublicCard[i] = m_szCardDeck[nDrawIndex++];
		}
		
		// 桌面数据 初始化
		m_nRound = 0;
		m_nBetNow = 0;
		m_nPotTotal = 0;

		m_nButtonIndex = clacButtonIndex();
		++m_nMatchCnt; // 比赛场次加一
		
		m_arrayPot.clear();
		
		// 发送每个人的个人信息 手牌信息
		for (Iterator<SeatInfo> it = m_listSeatInfo.iterator(); it.hasNext(); ) {
			SeatInfo seatInfo = it.next();
			if (seatInfo.player != null) {
				ClientMsg clientMsg = pkgSeatInfoMsg(seatInfo);
				clientMsg.setChannel(seatInfo.player.getChannel());
				PlayerMsgSender.getInstance().addMsg(clientMsg);
				
				sendHandCard(seatInfo.player);
			}
		}

		// 发送桌面信息
		sendPublicCardToAll();
		
		// update and send bCanCheck 
		m_bCanCheck = true;
		sendTableMsg(pkgCheckFlagMsg(m_bCanCheck));
		//sendCheckFlagToNextAll(getButtonIndex());

		int nButtonIndex = getButtonIndex();

		SeatInfo smallBlindSeat = findSeatFirstHand();
		if (smallBlindSeat == null) {
			log.error("not find small blind, end round");
			return ;
		}
		SeatInfo bigBlindSeat = findSeatNextActionByIndex(smallBlindSeat.nSeatIndex);
		if (bigBlindSeat == null)
		{
			log.error("not find big blind, end round");
			return ;
		}
		
		log.debug("start round[playerCnt=" + getPlayerCount()+ " match_cnt=" + m_nMatchCnt + " button_index=" + m_nButtonIndex + "]");
		// 大小盲注 压注
		m_nRaiseBeginIndex = smallBlindSeat.nSeatIndex;
		playerBet(smallBlindSeat, m_nBetMin / 2, ActionType.BLIND_BET);
		playerBet(bigBlindSeat, m_nBetMin, ActionType.BLIND_BET);
		
		nextPlayerDoing();
	}
	
	public void endRoundResult() {
		m_nRound = 4;
					
			
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null || seatInfo.nBet == 0)
				continue;
			
			// add bet in every pot, and try add player
			for (int j = 0; j < m_arrayPot.size(); ++j) {
				PotInfo potInfo = m_arrayPot.get(j);
				
				potInfo.tryAddPlayer(seatInfo);
				log.debug("texas try add in pot[seat_index=" + i + " uuid=" + seatInfo.player.getUuid() + " bet_limit=" + potInfo.nBetLimit + " bet=" + seatInfo.nBet + " action=" + seatInfo.eAction.value() + " status=" + seatInfo.eStatus.value() + "]");
			}
		}
		
		for(int i = 0; i < m_arrayPot.size(); ++i) {
			log.debug("pot info[i=" + i + " bet_limit=" + m_arrayPot.get(i).nBetLimit + " pot_value=" + m_arrayPot.get(i).nPotValue + 
					" player_cnt=" + m_arrayPot.get(i).arraySeat.size() + "]");
		}
		
		
		TreeMap<String, Winner> mapWinner = new TreeMap<String, Winner>(); // map<uuid, ...>
		/*
		for (int i = 0; i < m_arrayPot.size(); ++i) {
			PotInfo potInfo = m_arrayPot.get(i);
			long nCoinWin = potInfo.nPotValue / potInfo.arraySeat.size();
			for(int j = 0; j < potInfo.arraySeat.size(); ++j) {
				SeatInfo seatInfo = potInfo.arraySeat.get(j);
				Winner winner = mapWinner.get(seatInfo.player.getUuid());
				if (winner == null) {
					winner = new Winner(seatInfo, 0, seatInfo.nScore);
				}
				winner.nCoinWin += nCoinWin;
			}
		}*/
		
		
		//  ratio
		for (Map.Entry<String, Winner> entry : mapWinner.entrySet()) {
			Winner winner = entry.getValue();
			winner.nCoinWin = (long) (winner.nCoinWin * ( 1 - m_fRakeRate ));
			// reward coin
			PlayerDataService.getInstance().modifyCoin(winner.seatInfo.player, winner.nCoinWin, EventLogType.texas_reward);
		}
		
		// show hand card
		sendHandCardAtEnd();
		
		// send match result
		
		/*JSONObject jo = new JSONObject();
		
		JSONObject[] szJsonPot = new JSONObject[m_arrayPot.size()];
		for (int i = 0; i < m_arrayPot.size(); ++i) {
			JSONObject jsonPot = new JSONObject();
			jsonPot.put(Protocols.G2c_texas_match_result.Pot.COIN, m_arrayPot.get(i).nPotValue);
			
			JSONObject[] szJsonWinner = new JSONObject[m_arrayPot.get(i).arraySeat.size()];
			ArrayList<JSONObject> arrayTmp = new ArrayList<JSONObject>();
			for (int j = 0; j < m_arrayPot.get(i).arraySeat.size(); ++j) {
				SeatInfo seatInfo = m_arrayPot.get(i).arraySeat.get(j);
				JSONObject jsonSeat = new JSONObject();
				jsonSeat.put(Protocols.G2c_texas_match_result.Pot.Winner.UUID, seatInfo.player.getUuid());
				jsonSeat.put(Protocols.G2c_texas_match_result.Pot.Winner.SEAT_INDEX, seatInfo.nSeatIndex);
				szJsonWinner[j] = jsonSeat;
			}
			jsonPot.put(Protocols.G2c_texas_match_result.Pot.WINNER, szJsonWinner);
			
			szJsonPot[i] = jsonPot;
		}
		
		jo.put(Protocols.G2c_texas_match_result.POT, szJsonPot);
		jo.put(Protocols.MAINCODE, Protocols.G2c_texas_match_result.mainCode_value);
		jo.put(Protocols.SUBCODE, Protocols.G2c_texas_match_result.subCode_value);
		
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.setJson(jo);
		
		sendTableMsg(clientMsg);*/
		
		// send pot info
		JSONObject jo = pkgJsonPotInfo(new JSONObject());
		jo.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		jo.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.setJson(jo);
		sendTableMsg(clientMsg);
		
		
		// match result
		clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_match_result.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_match_result.subCode_value);
		
		sendTableMsg(clientMsg);
		
		// kick player 
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null)
				continue;
			
			if (seatInfo.player.getCoin() < m_nBetMin) {
				ClientMsg tmpMsg = pkgKickOutTableMsg(seatInfo.player.getUuid(), "金币不够，不能进入房间");
				tmpMsg.setChannel(seatInfo.player.getChannel());
				PlayerMsgSender.getInstance().addMsg(tmpMsg);
				
				removePlayer(seatInfo.player.getUuid());
			}
			
			if (seatInfo.eStatus == PlayerStatus.LEAVE) {
				removePlayer(seatInfo.player.getUuid());
			}
		}
		
		//super.gameEnd();
		this.table_status = STATUS_WAITING;
		
		log.debug("texas match end[" );
		
		//tryRunRound();
	}
	
	public void sendPlayerBetInfo(Player player, String strDesc) {
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_bet.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_bet.subCode_value);
		clientMsg.put(Protocols.G2c_texas_bet.DESC, strDesc);
		
		clientMsg.setChannel(player.getChannel());
		
		PlayerMsgSender.getInstance().addMsg(clientMsg);
	}
	
	public void sendErrorMsg(Player player, int nSubCode, String strError) {
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_enter_room.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, nSubCode);
		clientMsg.put(Protocols.ERRORCODE, strError);
		
		clientMsg.setChannel(player.getChannel());
		
		PlayerMsgSender.getInstance().addMsg(clientMsg);
	}
	
	public ClientMsg pkgKickOutTableMsg(String strUuid, String strDesc) {
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_kick_out_table.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_kick_out_table.subCode_value);
		clientMsg.put(Protocols.G2c_texas_kick_out_table.UUID, strUuid);
		clientMsg.put(Protocols.G2c_texas_kick_out_table.DESC, strDesc);
		
		return clientMsg;
	}
	
	public ClientMsg pkgTableInfoMsg() {
		JSONObject jo = new JSONObject();
		jo.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		jo.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		
		/*jo.put(Protocols.G2c_texas_table_info.ROUND, m_nRound);
		int nButtonIndex = getButtonIndex();
		jo.put(Protocols.G2c_texas_table_info.BUTTON_INDEX, nButtonIndex);
		jo.put(Protocols.G2c_texas_table_info.TABLE_CARD1, m_szPublicCard[0]);
		jo.put(Protocols.G2c_texas_table_info.TABLE_CARD2, m_szPublicCard[1]);
		jo.put(Protocols.G2c_texas_table_info.TABLE_CARD3, m_szPublicCard[2]);
		jo.put(Protocols.G2c_texas_table_info.TABLE_CARD4, m_szPublicCard[3]);
		jo.put(Protocols.G2c_texas_table_info.TABLE_CARD5, m_szPublicCard[4]);*/
		
		jo = pkgPublicTableInfo(jo);
		
		// pkg pot info
		JSONObject[] szPot = new JSONObject[m_arrayPot.size()];
		for (int i = 0; i < m_arrayPot.size(); ++i) {
			PotInfo pot = m_arrayPot.get(i);
			JSONObject tmpJObj = new JSONObject();
			tmpJObj.put(Protocols.G2c_texas_table_info.Pot.COIN, pot.nPotValue);
			//tmpJObj.put(Protocols.G2c_texas_table_info.Pot.UUID, pot.strPlayerUuid);
			szPot[i] = tmpJObj;
		}
		jo.put(Protocols.G2c_texas_table_info.POT, szPot);
		
		// pkg player info
		ArrayList<JSONObject> arrayPlayer = new ArrayList<JSONObject>();
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo tmpInfo = m_listSeatInfo.get(i);
			if ( tmpInfo.player == null ) continue;
			/*JSONObject tmpObj = new JSONObject();
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.UUID, tmpInfo.player.getUuid());
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.NAME, tmpInfo.player.getHead());
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.NAME, tmpInfo.player.getName());
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.SEAT_INDEX, i);
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.STATUS, tmpInfo.eStatus.value());
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.BET, tmpInfo.nBet);
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.COIN, tmpInfo.player.getCoin());
			tmpObj.put(Protocols.G2c_texas_table_info.Player_info.ACTION, tmpInfo.eAction.value() );
			*/
			JSONObject tmpObj = pkgJsonSeatInfo(tmpInfo);
			arrayPlayer.add(tmpObj);
		}
		JSONObject[] szPlayer = arrayPlayer.toArray(new JSONObject[arrayPlayer.size()]);
		jo.put(Protocols.G2c_texas_table_info.PLAYER_INFO, szPlayer);
		
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.setJson(jo);
		
		return clientMsg;
	}
	
	public JSONObject pkgPublicTableInfo(JSONObject jo) {
	
		int nCard1 = -1;
		int nCard2 = -1;
		int nCard3 = -1;
		int nCard4 = -1;
		int nCard5 = -1;
		if (m_nRound >= 1) {
			// 发三张牌
			nCard1 = m_szPublicCard[0];
			nCard2 = m_szPublicCard[1];
			nCard3 = m_szPublicCard[2];
		}
		if (m_nRound >= 2) {
			nCard4 = m_szPublicCard[3];
		}
		if (m_nRound >= 3) {
			nCard5 = m_szPublicCard[4];
		}
				
		jo.put(Protocols.G2c_texas_table_info.ROUND, m_nRound);

		if (m_nRound >= 0) { // has start
			jo.put(Protocols.G2c_texas_table_info.BUTTON_INDEX, getButtonIndex());
			jo.put(Protocols.G2c_texas_table_info.TABLE_CARD1, nCard1);
			jo.put(Protocols.G2c_texas_table_info.TABLE_CARD2, nCard2);
			jo.put(Protocols.G2c_texas_table_info.TABLE_CARD3, nCard3);
			jo.put(Protocols.G2c_texas_table_info.TABLE_CARD4, nCard4);
			jo.put(Protocols.G2c_texas_table_info.TABLE_CARD5, nCard5);
		}

		return jo;
	}
	
	public ClientMsg pkgSeatInfoMsg(SeatInfo seatInfo) {
		
		if (seatInfo == null || seatInfo.player == null)
			return null;
		
		String strUuid = seatInfo.player.getUuid();
		int	nHead = seatInfo.player.getHead();
		String strName = seatInfo.player.getName();
		long nCoin = seatInfo.player.getCoin();
		
		JSONObject[] szObj = new JSONObject[1];
		szObj[0] = new JSONObject();

		/*JSONObject tmpObj = new JSONObject();
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.UUID, strUuid);
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.HEAD, nHead);
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.NAME, strName);
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.SEAT_INDEX, seatInfo.nSeatIndex);
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.STATUS, seatInfo.eStatus.value());
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.BET, seatInfo.nBet);
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.COIN, nCoin);
		tmpObj.put(Protocols.G2c_texas_table_info.Player_info.ACTION, seatInfo.eAction.value());
		*/
		
		szObj[0] = pkgJsonSeatInfo(seatInfo);
		
		JSONObject jo = new JSONObject();
		jo.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		jo.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		jo.put(Protocols.G2c_texas_table_info.PLAYER_INFO, szObj);	
		
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.setJson(jo);
		return clientMsg;
	}
	
	public JSONObject pkgJsonSeatInfo(SeatInfo seatInfo) {
		
		JSONObject jo = new JSONObject();
		jo.put(Protocols.G2c_texas_table_info.Player_info.UUID, seatInfo.player.getUuid());
		jo.put(Protocols.G2c_texas_table_info.Player_info.HEAD, seatInfo.player.getHead());
		jo.put(Protocols.G2c_texas_table_info.Player_info.NAME, seatInfo.player.getName());
		jo.put(Protocols.G2c_texas_table_info.Player_info.SEAT_INDEX, seatInfo.nSeatIndex);
		jo.put(Protocols.G2c_texas_table_info.Player_info.STATUS, seatInfo.eStatus.value());
		jo.put(Protocols.G2c_texas_table_info.Player_info.BET, seatInfo.nBet);
		jo.put(Protocols.G2c_texas_table_info.Player_info.COIN, seatInfo.player.getCoin());
		jo.put(Protocols.G2c_texas_table_info.Player_info.ACTION, seatInfo.eAction.value());
		
		return jo;
	}
	
	public void sendHandCardAtEnd() {
		
		ClientMsg clientMsg = new ClientMsg();
		
		ArrayList<JSONObject> arrayJsonObj = new ArrayList<JSONObject>();
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player != null && (seatInfo.canReward())) {
				JSONObject tmpObj = new JSONObject();
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.UUID, seatInfo.player.getUuid());
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.CARD1, seatInfo.nCard1);
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.CARD2, seatInfo.nCard2);
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.RESULT, TexasUtil.getCardResultByScore(seatInfo.nScore));
				arrayJsonObj.add(tmpObj);
			}
		}
		
		if (arrayJsonObj.size() == 0) {
			return;
		}
		
		JSONObject[] szJsonObj = arrayJsonObj.toArray(new JSONObject[arrayJsonObj.size()]);
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_hand_card.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_hand_card.subCode_value);
		clientMsg.put(Protocols.G2c_texas_hand_card.HAND_CARD, szJsonObj);
	
		sendTableMsg(clientMsg);
	}
	
	public void sendHandCard(Player player) {
		ClientMsg clientMsg = new ClientMsg();
		
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_hand_card.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_hand_card.subCode_value);
		
		ArrayList<JSONObject> arrayJsonObj = new ArrayList<JSONObject>();
		
		for (Iterator<SeatInfo> it = m_listSeatInfo.iterator(); it.hasNext(); ) {
			SeatInfo seatInfo = it.next();
			if ((seatInfo.player != null && seatInfo.player.getUuid() == player.getUuid()) || seatInfo.bShowCard == true) {
				JSONObject tmpObj = new JSONObject();
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.UUID, seatInfo.player.getUuid());
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.CARD1, seatInfo.nCard1);
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.CARD2, seatInfo.nCard2);
				
				arrayJsonObj.add(tmpObj);
			}
		}
		
		if (arrayJsonObj.size() == 0) {
			return ; 
		}
		
		JSONObject[] szJsonObj = arrayJsonObj.toArray(new JSONObject[arrayJsonObj.size()]);
		
		clientMsg.put(Protocols.G2c_texas_hand_card.HAND_CARD, szJsonObj);
		
		clientMsg.setChannel(player.getChannel());
		
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
	}

	
	public void sendPublicCardToAll() {
		
		JSONObject jo = pkgPublicTableInfo(new JSONObject());
		
		ClientMsg clientMsg = new ClientMsg();
		
		jo.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		jo.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		clientMsg.setJson(jo);
		
		sendTableMsg(clientMsg);
	}
	
	public void initPot() {
		TreeSet<Long> setBetSplit = new TreeSet<Long>();
		
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			
			if (seatInfo.player != null && seatInfo.nBet > 0) {
				if (seatInfo.eAction != ActionType.FOLD)
					setBetSplit.add(seatInfo.nBet);
			}
		}
		
		m_arrayPot.clear();
		for (Iterator<Long> it = setBetSplit.iterator(); it.hasNext(); ) {
			long nBetLimit = (Long)it.next();
			PotInfo potInfo = new PotInfo(nBetLimit);
			m_arrayPot.add(potInfo);
		}
		
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null || seatInfo.nBet == 0)
				continue;
		
			// add bet in every pot

			long nBetLeft = seatInfo.nBet;
			for (int j = 0; j < m_arrayPot.size(); ++j) {
				PotInfo potInfo = m_arrayPot.get(j);
				long nAddLimit;
				if ( j > 0 )
					nAddLimit = potInfo.nBetLimit - m_arrayPot.get(j - 1).nBetLimit;
				else
					nAddLimit = potInfo.nBetLimit;
				
				if (nBetLeft > nAddLimit) {
					potInfo.nPotValue += nAddLimit;
					nBetLeft = nBetLeft - nAddLimit;
				} else {
					potInfo.nPotValue += nBetLeft;
					nBetLeft = 0;
				}
				
				if (nBetLeft == 0)
					break;
			}
		}
	}
	
	
	public void updatePot() {
		TreeSet<Long> setBetSplit = new TreeSet<Long>();
		
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			
			if (seatInfo.player != null && seatInfo.nBet > 0) {
				if (seatInfo.eAction != ActionType.FOLD)
					setBetSplit.add(seatInfo.nBet);
			}
		}
		
		m_arrayPot.clear();
		for (Iterator<Long> it = setBetSplit.iterator(); it.hasNext(); ) {
			long nBetLimit = (Long)it.next();
			PotInfo potInfo = new PotInfo(nBetLimit);
			m_arrayPot.add(potInfo);
		}
		
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null || seatInfo.nBet == 0)
				continue;
		
			// add bet in every pot

			long nBetLeft = seatInfo.nBet;
			for (int j = 0; j < m_arrayPot.size(); ++j) {
				PotInfo potInfo = m_arrayPot.get(j);
				long nAddLimit;
				if ( j > 0 )
					nAddLimit = potInfo.nBetLimit - m_arrayPot.get(j - 1).nBetLimit;
				else
					nAddLimit = potInfo.nBetLimit;
				
				if (nBetLeft > nAddLimit) {
					potInfo.nPotValue += nAddLimit;
					nBetLeft = nBetLeft - nAddLimit;
				} else {
					potInfo.nPotValue += nBetLeft;
					nBetLeft = 0;
				}
				
				if (nBetLeft == 0)
					break;
			}
		}
		
		/*
		log.debug("update pot[pot_size=" + m_arrayPot.size());
		for (int i = 0; i < m_arrayPot.size(); ++i) {
			PotInfo potInfo = m_arrayPot.get(i);
			log.debug("update pot[bet_limit=" + potInfo.nBetLimit + " pot_value=" + potInfo.nPotValue);
		}*/
	}
	
	public void sendPotTotalToAll() {
	
		ClientMsg clientMsg = new ClientMsg();
		
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		clientMsg.put(Protocols.G2c_texas_table_info.POT_TOTAL, m_nPotTotal);
	
		sendTableMsg(clientMsg);
		
	}
	
	public JSONObject pkgJsonPotInfo(JSONObject jo) {
				
		JSONObject[] szJsonPot = new JSONObject[m_arrayPot.size()];
		for (int i = 0; i < m_arrayPot.size(); ++i) {
			JSONObject jsonPot = new JSONObject();
			jsonPot.put(Protocols.G2c_texas_table_info.Pot.COIN, m_arrayPot.get(i).nPotValue);
			
			JSONObject[] szJsonWinner = new JSONObject[m_arrayPot.get(i).arraySeat.size()];
			for (int j = 0; j < m_arrayPot.get(i).arraySeat.size(); ++j) {
				SeatInfo seatInfo = m_arrayPot.get(i).arraySeat.get(j);
				JSONObject jsonSeat = new JSONObject();
				jsonSeat.put(Protocols.G2c_texas_table_info.Pot.Winner.UUID, seatInfo.player.getUuid());
				jsonSeat.put(Protocols.G2c_texas_table_info.Pot.Winner.SEAT_INDEX, seatInfo.nSeatIndex);
				szJsonWinner[j] = jsonSeat;
			}
			jsonPot.put(Protocols.G2c_texas_table_info.Pot.WINNER, szJsonWinner);
			
			szJsonPot[i] = jsonPot;
		}
		
		jo.put(Protocols.G2c_texas_table_info.POT, szJsonPot);

		return jo;
	}
	
	public void sendCheckFlagToNextAll(int nIndexNow) {
		
		SeatInfo seatInfo = findSeatCanAction(nIndexNow + 1, m_nRaiseBeginIndex);
		//log.debug("texas send begin------------------[index=" + nIndexNow);
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			if (seatInfo == null)
				break;

			sendCheckFlag(seatInfo, m_bCanCheck);	
			seatInfo = findSeatCanAction(seatInfo.nSeatIndex + 1, m_nRaiseBeginIndex);
			
		}

	}
	
	public void sendCheckFlag(SeatInfo seatInfo, boolean bCanCheck) {

		ClientMsg clientMsg = pkgCheckFlagMsg(bCanCheck);
		clientMsg.setChannel(seatInfo.player.getChannel());
		
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
		//log.debug("texas send check[index=" + seatInfo.nSeatIndex + " uuid=" + seatInfo.player.getUuid() + " can_check=" + bCanCheck );
	}
	
	public ClientMsg pkgCheckFlagMsg(boolean bCanCheck) {
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_round_info.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_round_info.subCode_value);
		clientMsg.put(Protocols.G2c_texas_round_info.IS_CHECK, bCanCheck);
		
		return clientMsg;
	}
	/*
	public void sendHandCardResult(Player player) {
		ClientMsg clientMsg = new ClientMsg();
		
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_hand_card.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_hand_card.subCode_value);
		
		ArrayList<JSONObject> arrayJsonObj = new ArrayList<JSONObject>();
		
		for (Iterator<SeatInfo> it = m_listSeatInfo.iterator(); it.hasNext(); ) {
			SeatInfo seatInfo = it.next();
			if ((seatInfo.player != null && seatInfo.player.getUuid() == player.getUuid()) || seatInfo.bShowCard == true) {
				JSONObject tmpObj = new JSONObject();
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.UUID, seatInfo.player.getUuid());
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.RESULT, TexasUtil.getCardResultByScore(seatInfo.nScore));
				
				arrayJsonObj.add(tmpObj);
			}
		}
		
		if (arrayJsonObj.size() == 0) {
			return ; 
		}
		
		JSONObject[] szJsonObj = arrayJsonObj.toArray(new JSONObject[arrayJsonObj.size()]);
		
		clientMsg.put(Protocols.G2c_texas_hand_card.HAND_CARD, szJsonObj);
		
		clientMsg.setChannel(player.getChannel());
		
		PlayerMsgSender.getInstance().addMsg(clientMsg);
	}*/
	public void updateAndSendHandCard() {
		int nNum = 0;
		if (m_nRound == 1) {
			nNum = 3;
		} else if (m_nRound == 2) {
			nNum = 4;
		} else if (m_nRound == 3) {
			nNum = 5;
		}
		updateAndSendHandCard(nNum);
	}
	
	public void updateAndSendHandCard(int nNum) {
		ArrayList<JSONObject> arrayJsonObj = new ArrayList<JSONObject>();
		
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null) 
				continue;
			seatInfo.nScore = calcPlayer(seatInfo, nNum);
			
			if (seatInfo.bShowCard == true) {
				// pkg data
				JSONObject tmpObj = new JSONObject();
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.UUID, seatInfo.player.getUuid());
				tmpObj.put(Protocols.G2c_texas_hand_card.Hand_card.RESULT, TexasUtil.getCardResultByScore(seatInfo.nScore));
				
				arrayJsonObj.add(tmpObj);
			}
		}
		
		// send data to every player
		for (int i = 0; i < m_listSeatInfo.size(); ++i) {
			SeatInfo seatInfo = m_listSeatInfo.get(i);
			if (seatInfo.player == null)
				continue;
			
			JSONObject JsonMy = null;
			if (seatInfo.bShowCard == false) {
				JsonMy = new JSONObject();
				JsonMy.put(Protocols.G2c_texas_hand_card.Hand_card.UUID, seatInfo.player.getUuid());
				JsonMy.put(Protocols.G2c_texas_hand_card.Hand_card.RESULT, TexasUtil.getCardResultByScore(seatInfo.nScore));
				
			}
			
			int nSize = JsonMy == null ? arrayJsonObj.size() : (arrayJsonObj.size() + 1);

			JSONObject[] szJsonObj = new JSONObject[nSize];
			
			for (int j = 0; j < arrayJsonObj.size(); ++j) {
				szJsonObj[j] = arrayJsonObj.get(j);
			}
			if (JsonMy != null) {
				szJsonObj[nSize - 1] = JsonMy;
			}
			
			ClientMsg clientMsg = new ClientMsg();
			clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_hand_card.mainCode_value);
			clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_hand_card.subCode_value);
			clientMsg.put(Protocols.G2c_texas_hand_card.HAND_CARD, szJsonObj);
			
			clientMsg.setChannel(seatInfo.player.getChannel());
			PlayerMsgSender.getInstance().addMsg(clientMsg);
			
		}
	}
	
	public void updatePlayerStatus(SeatInfo seatInfo, PlayerStatus e) {
		if (seatInfo == null || seatInfo.player == null)
			return;
		
		seatInfo.eStatus = e;
		
		JSONObject[] szObj = new JSONObject[1];
		szObj[0] = pkgJsonSeatInfo(seatInfo);

		ClientMsg clientMsg = new ClientMsg();
		clientMsg.put(Protocols.MAINCODE, Protocols.G2c_texas_table_info.mainCode_value);
		clientMsg.put(Protocols.SUBCODE, Protocols.G2c_texas_table_info.subCode_value);
		clientMsg.put(Protocols.G2c_texas_table_info.PLAYER_INFO, szObj);
		
		sendTableMsg(clientMsg);
		
	}
}


