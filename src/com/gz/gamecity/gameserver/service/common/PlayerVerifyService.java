package com.gz.gamecity.gameserver.service.common;

import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

import io.netty.util.Attribute;

public class PlayerVerifyService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg msg) {
		ProtocolMsg pMsg=(ProtocolMsg)msg;
		int subCode = pMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.L2g_playerVerify.subCode_value:
			handlerVerifyPlayer(pMsg);
			break;
		case Protocols.L2g_kickPlayer.subCode_value:
			kickPlayer(pMsg);
			break;
		}

	}

	private void handlerVerifyPlayer(ProtocolMsg pMsg) {

		ClientMsg cMsg = new ClientMsg();
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_login.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_login.subCode_value);
		String uuid = pMsg.getJson().getString(Protocols.L2g_playerVerify.UUID);
		String gameToken = pMsg.getJson().getString(Protocols.L2g_playerVerify.GAMETOKEN);
		
		Player player = PlayerManager.getInstance().getLoginPlayer(uuid+gameToken);
		if (player != null) {
			String err = pMsg.getJson().getString(Protocols.ERRORCODE);
			if (err != null && !err.equals("")) { // 验证失败
				cMsg.put(Protocols.ERRORCODE, err);
				player.write(cMsg.getJson()); // 这里就直接返回失败然后关闭连接，如果塞入发送队列，就无法关闭连接
				player.getChannel().close();
				PlayerManager.getInstance().playerOffline(player.getUuid());
				return;
			}
			
			if(PlayerManager.getInstance().getOnlinePlayer(uuid)!=null){
				// 如果玩家数据已经在线，通常是顶号，或者是掉线重连
				player = PlayerManager.getInstance().playerReconnect(player);
			}
			else{
				JSONObject j = pMsg.getJson();
				player.setName(j.getString(Protocols.L2g_playerVerify.NAME));
				player.setCoin(j.getLongValue(Protocols.L2g_playerVerify.COIN));
				player.setVip(j.getIntValue(Protocols.L2g_playerVerify.VIP));
				player.setCharge_total(j.getIntValue(Protocols.L2g_playerVerify.CHARGE_TOTAL));
				player.setSex(j.getByteValue(Protocols.L2g_playerVerify.SEX));
				player.setLvl(j.getIntValue(Protocols.L2g_playerVerify.LVL));
				player.setCharm(j.getIntValue(Protocols.L2g_playerVerify.CHARM));
				player.setFinance(j.getIntValue(Protocols.L2g_playerVerify.FINANCE));
				player.setHead(j.getIntValue(Protocols.L2g_playerVerify.HEAD));
				player.setSign(j.getString(Protocols.L2g_playerVerify.SIGN));
				PlayerManager.getInstance().playerOnline(player);
			}
			
			cMsg.setChannel(player.getChannel());
			cMsg.put(Protocols.G2c_login.NAME, player.getName());
			cMsg.put(Protocols.G2c_login.COIN, player.getCoin());
			cMsg.put(Protocols.G2c_login.VIP, player.getVip());
			cMsg.put(Protocols.G2c_login.CHARGE_TOTAL, player.getCharge_total());
			cMsg.put(Protocols.G2c_login.SEX, player.getSex());
			cMsg.put(Protocols.G2c_login.LVL, player.getLvl());
			cMsg.put(Protocols.G2c_login.CHARM, player.getCharm());
			cMsg.put(Protocols.G2c_login.FINANCE, player.getFinance());
			cMsg.put(Protocols.G2c_login.HEAD, player.getHead());
			cMsg.put(Protocols.G2c_login.SIGN, player.getSign());
			cMsg.put(Protocols.G2c_login.TIMESTAMP, System.currentTimeMillis());
			PlayerMsgSender.getInstance().addMsg(cMsg);
			
		}

	}

	private void kickPlayer(ProtocolMsg pMsg) {
		String uuid = pMsg.getJson().getString(Protocols.L2g_kickPlayer.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player!=null){
			player.getChannel().close();
		}
		
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYERVERIFY;
	}
	
	

}
