package com.gz.gamecity.gameserver.service.common;

import java.util.concurrent.ConcurrentHashMap;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.JsonUtil;
import com.gz.util.SensitivewordFilter;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class ChatService implements LogicHandler{

	@Override
	public void handleMsg(BaseMsg msg) {
		// TODO Auto-generated method stub
		Player player=PlayerManager.getPlayerFromMsg(msg);
		if(player==null){
			msg.closeChannel();
			return;
		}
		ClientMsg cMsg=(ClientMsg)msg;
		int subCode = cMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch(subCode){
		case Protocols.C2g_send_msg.subCode_value:
			handleChatMsg(player,cMsg);
			break;
		}
	}

	private void handleChatMsg(Player player, ClientMsg cMsg) {
		if(!allowedSend(player)){
			return;
		}
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_send_msg.subCode_value);
		cMsg.put("uuid",player.getUuid());
		cMsg.put("name",player.getName());
		cMsg.put("head",player.getHead());
		cMsg.put("finance",player.getFinance());
		cMsg.put("lvl",player.getLvl());
		cMsg.put("vip",player.getVip());
		
		String msg = cMsg.getJson().getString("revMsg");
		
		SensitivewordFilter filter = SensitivewordFilter.getInstance();
		String _str = filter.replaceSensitiveWord(msg, 0, "*");
		
		cMsg.put("revMsg",_str);
		
		ConcurrentHashMap<String, Player> map_players = PlayerManager.getInstance().getOnlinePlayers();
		for(Player p:map_players.values()){
			if(p.isOnline()){
				ClientMsg _msg = new  ClientMsg();
				_msg.setChannel(p.getChannel());
				_msg.setJson(JsonUtil.copyJson(cMsg.getJson()));
				PlayerMsgSender.getInstance().addMsg(_msg);
			}
		}
	}
	
	private boolean allowedSend(Player player){
		// TODO 判断是否可以发送，主要是扣钱之类的
		return true;
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.CHAT;
	}

}
