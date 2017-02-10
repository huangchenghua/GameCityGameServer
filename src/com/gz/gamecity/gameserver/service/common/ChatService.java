package com.gz.gamecity.gameserver.service.common;

import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.JsonUtil;
import com.gz.util.SensitivewordFilter;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class ChatService implements LogicHandler{
	
	private static ChatService instance;
	
	public static synchronized ChatService getInstance() {
		if(instance == null)
			instance = new ChatService();
		return instance;
	}

	private ChatService(){}

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
		if(player.isSilent())
		{
			cMsg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str4"));
			PlayerMsgSender.getInstance().addMsg(cMsg);
			return;
		}
		boolean horn = cMsg.getJson().getBooleanValue(Protocols.C2g_send_msg.HORN);
		if(!allowedSend(player,horn)){
			return;
		}
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_send_msg.subCode_value);
		cMsg.put(Protocols.G2c_send_msg.UUID,player.getUuid());
		cMsg.put(Protocols.G2c_send_msg.NAME,player.getName());
		cMsg.put(Protocols.G2c_send_msg.HEAD,player.getHead());
		cMsg.put(Protocols.G2c_send_msg.FINANCE,player.getFinance());
		cMsg.put(Protocols.G2c_send_msg.LVL,player.getLvl());
		cMsg.put(Protocols.G2c_send_msg.VIP,player.getVip());
		cMsg.put(Protocols.G2c_send_msg.HORN,horn);
		
		String msg = cMsg.getJson().getString("revMsg");
		
		SensitivewordFilter filter = SensitivewordFilter.getInstance();
		String _str = filter.replaceSensitiveWord(msg, 0, "*");
		
		cMsg.put(Protocols.G2c_send_msg.REVMSG,_str);
		
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
	
	public void sendGameMsg(String msg){
		ClientMsg cMsg = new ClientMsg();
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_system_msg.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_system_msg.subCode_value);
		cMsg.put(Protocols.G2c_system_msg.REVMSG, msg);
		
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
	
	private boolean allowedSend(Player player,Boolean horn){
		// TODO 判断是否可以发送，主要是扣钱之类的
		int vip = player.getVip();
		long coin = player.getCoin();
		
		JSONArray simple_chat_level = AllTemplate.getsimple_chat_levelArray();
		JSONArray horn_chat_level = AllTemplate.gethorn_chat_levelArray();
		
		if(horn == true){
			if(vip >= Integer.parseInt(((JSONObject)horn_chat_level.get(0)).getString("level"))){
				if(vip == Integer.parseInt(((JSONObject)horn_chat_level.get(0)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)horn_chat_level.get(0)).getString("cash"));
					if(coin > cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip == Integer.parseInt(((JSONObject)horn_chat_level.get(1)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)horn_chat_level.get(1)).getString("cash"));
					if(coin>cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip == Integer.parseInt(((JSONObject)horn_chat_level.get(2)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)horn_chat_level.get(2)).getString("cash"));
					if(coin>cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip == Integer.parseInt(((JSONObject)horn_chat_level.get(3)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)horn_chat_level.get(3)).getString("cash"));
					if(coin>cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip == Integer.parseInt(((JSONObject)horn_chat_level.get(4)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)horn_chat_level.get(4)).getString("cash"));
					if(coin>cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip > Integer.parseInt(((JSONObject)horn_chat_level.get(4)).getString("level"))){
					return true;
				}
			}
		}else if(horn == false){
			if(vip >= Integer.parseInt(((JSONObject)simple_chat_level.get(0)).getString("level"))){
				if(vip == Integer.parseInt(((JSONObject)simple_chat_level.get(0)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)simple_chat_level.get(0)).getString("cash"));
					if(coin>=cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip == Integer.parseInt(((JSONObject)simple_chat_level.get(1)).getString("level"))){
					long cash = Long.parseLong(((JSONObject)simple_chat_level.get(1)).getString("cash"));
					if(coin>=cash){
						PlayerDataService.getInstance().modifyCoin(player,-cash,EventLogType.chat);
						return true;
					}
				}else if(vip > Integer.parseInt(((JSONObject)simple_chat_level.get(1)).getString("level"))){
					return true;
				}
			}
		}		
		return false;
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.CHAT;
	}

}
