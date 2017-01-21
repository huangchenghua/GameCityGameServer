package com.gz.gamecity.gameserver.service.common;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.DateUtil;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class AlmsHandler implements LogicHandler {
	
	private static final Logger log = Logger.getLogger(AlmsHandler.class);
	
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
		case Protocols.C2g_friend_add.subCode_value :
			handleEnterHall(player, clientMsg);
			break;
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
	
	private int getAlmsCntToday(Player player) {
		
		long diff = -1;
		try {
			diff = DateUtil.dateDays(player.getAlmsTime(), DateUtil.getCurDateTime("yyyy-MM-dd"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (diff > 0) {
			return 0;
		}
		return player.getAlmsCnt();
	}
	
	private void handleEnterHall(Player player, ClientMsg cliengMsg) {
		JSONObject joConfig = getConfig(player.getVip());
		if (joConfig == null) {
			log.error("[alms] conf not find[uuid=" + player.getUuid() + " vip=" + player.getVip());
			return;
		}
		
	}
}
