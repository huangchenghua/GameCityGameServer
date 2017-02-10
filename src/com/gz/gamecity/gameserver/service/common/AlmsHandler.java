package com.gz.gamecity.gameserver.service.common;

import javax.xml.stream.events.Characters;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.DateUtil;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class AlmsHandler implements LogicHandler {
	
	private static final Logger log = Logger.getLogger(AlmsHandler.class);
	
	private final static int N_REQ_COIN = 2000;
	
	@Override
	public void handleMsg(BaseMsg msg) {
		Player player = null;
		if (!msg.isInner()) {
			player = PlayerManager.getPlayerFromMsg(msg);
			if (player == null) {
				log.error("[alms] player not find, close websocket");
				msg.closeChannel();
				return;
			}
		}
		
		ClientMsg clientMsg = (ClientMsg)msg;
		
		int subCode = clientMsg.getJson().getIntValue(Protocols.SUBCODE);
		
		String strUuid = player == null ? "null" : player.getUuid();
		log.debug("handle alms [uuid=" + strUuid + " subCode=" + subCode + "]");
		
		switch (subCode) {
		case Protocols.C2g_alms_enter_hall.subCode_value :
			handleEnterHall(player, clientMsg);
			break;
		case Protocols.C2g_alms_get_reward.subCode_value :
			handleGetReward(player, clientMsg);
			break;
		case Protocols.C2g_charts_get_list.subCode_value :
			handleChartsGetList(player, clientMsg);
		default :
			log.error("[alms] not find subCode: " + subCode);
		}
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.ALMS;
	}

	private JSONObject getConfig(int nVip) {
		JSONArray ja = AllTemplate.getvipLevel_jsonArray();
		for (int i = 0; i < ja.size(); ++i) {
			JSONObject jo = ja.getJSONObject(i);
			if (nVip == jo.getIntValue("vip"))
				return jo;
		}
		return null;
	}
	
	private byte getAlmsCntToday(Player player) {
		if(player.getAlmsTime()== null)
			return 0;
		long diff = 0;
		try {
			//diff = DateUtil.dateDays(player.getAlmsTime(), DateUtil.getCurDateTime("yyyy-MM-dd"));
			diff = DateUtil.dateDays2(player.getAlmsTime(), DateUtil.getCurDateTime());
		} catch (Exception e) {
			//e.printStackTrace();
		}
		if (diff > 0) {
			return 0;
		}
		
		log.debug("alms time[uuid=" + player.getUuid() + "diff=" + diff + " alms_time=" + player.getAlmsTime() + " cur_time=" + DateUtil.getCurDateTime() + " alms_cnt=" + player.getAlmsCnt() + "]");
		return player.getAlmsCnt();
	}
	
	private void handleEnterHall(Player player, ClientMsg cliengMsg) {
		JSONObject joConfig = getConfig(player.getVip());
		if (joConfig == null) {
			log.error("[alms] conf not find[uuid=" + player.getUuid() + " vip=" + player.getVip());
			return;
		}
		int nLeftCnt = joConfig.getIntValue("alms_cnt") - getAlmsCntToday(player);
		if (nLeftCnt < 0)
			nLeftCnt = 0;
		
		long nCoinReward = joConfig.getLongValue("alms_coin");
		
		if (nLeftCnt == 0 || player.getCoin() >= N_REQ_COIN)
			return;
		
		ClientMsg msg = new ClientMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2c_alms_show_panel.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2c_alms_show_panel.subCode_value);
		msg.put(Protocols.G2c_alms_show_panel.LEFT_CNT, nLeftCnt);
		msg.put(Protocols.G2c_alms_show_panel.COIN_REWARD, nCoinReward);
		msg.put(Protocols.G2c_alms_show_panel.TOTAL_CNT, joConfig.getIntValue("alms_cnt"));
		msg.put(Protocols.G2c_alms_show_panel.REQ_COIN, N_REQ_COIN);
		msg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg);
	}
	
	private void handleGetReward(Player player, ClientMsg clientMsg) {
		ClientMsg msg = new ClientMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2c_alms_get_reward.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2c_alms_get_reward.subCode_value);
		msg.setChannel(player.getChannel());
		
		if (player.getCoin() >= N_REQ_COIN) {
			log.error("[alms] coin is enough, not get reward[uuid=" + player.getUuid() + " vip=" + player.getVip() + " coin=" + player.getCoin());
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str1"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return ;
		}
		
		JSONObject joConfig = getConfig(player.getVip());
		if (joConfig == null) {
			log.error("[alms] conf not find[uuid=" + player.getUuid() + " vip=" + player.getVip());
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str2"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		byte nAlmsCnt = getAlmsCntToday(player);
		if (nAlmsCnt >= joConfig.getIntValue("alms_cnt")) {
			log.error("[alms] alms cnt is overtop [uuid=" + player.getUuid() + " alms_cnt=" + nAlmsCnt);
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str3"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return ;
		}
		
		PlayerDataService.getInstance().modifyCoin(player, joConfig.getLongValue("alms_coin"), EventLogType.alms_reward);
		PlayerDataService.getInstance().setAlms(player, (byte)(nAlmsCnt + 1), DateUtil.getCurDateTime());
		
		PlayerMsgSender.getInstance().addMsg(msg);
		
	}
	
	private void handleChartsGetList(Player player, ClientMsg clientMsg) {
		if (!clientMsg.getJson().containsKey(Protocols.C2g_charts_get_list.CHARTS_TYPE)) {
			log.error("handle charts get list, error client data[uuid=" + player.getUuid() + "]");
			return;
		}
		int nChartsType = clientMsg.getJson().getIntValue(Protocols.C2g_charts_get_list.CHARTS_TYPE);
		
		ProtocolMsg msg = new ProtocolMsg();
		
		msg.put(Protocols.G2l_charts_get_list.UUID_SELF, player.getUuid());
		msg.put(Protocols.G2l_charts_get_list.CHARTS_TYPE, nChartsType);
		
		msg.put(Protocols.MAINCODE, Protocols.G2l_charts_get_list.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_charts_get_list.subCode_value);

		LoginMsgSender.getInstance().addMsg(msg);
		
		log.debug("handle charts [uuid_self=" + player.getUuid() + " chartsType=" + nChartsType + "]");
	}
	
}
