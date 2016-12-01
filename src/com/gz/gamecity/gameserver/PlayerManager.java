package com.gz.gamecity.gameserver;

import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.DelayCache;
import com.gz.websocket.msg.ProtocolMsg;

public class PlayerManager {
	
	private static final long loginCacheTime = 10*60*1000l;
	
	
	private static PlayerManager instance;

	private ConcurrentHashMap<String,Player> onlinePlayers;
	
	private DelayCache<String, Player> logoutCache;
	
	public static synchronized PlayerManager getInstance() {
		if(instance==null)
			instance=new PlayerManager();
		return instance;
	}
	
	private PlayerManager(){
		onlinePlayers=new ConcurrentHashMap<>();
		logoutCache=new DelayCache<String, Player>();
	}
	
	public void playerOnline(Player player){
		onlinePlayers.put(player.getUuid(), player);
	}
	
	public Player getOnlinePlayer(String uuid){
		return onlinePlayers.get(uuid);
	}
	
	public Player playerOffline(String uuid){
		Player player = onlinePlayers.remove(uuid);
		if(player!=null){
			this.playerLogout(player);
			ProtocolMsg msg=new ProtocolMsg();
			JSONObject json=new JSONObject();
			json.put(Protocols.MAINCODE, Protocols.G2l_playerLogout.mainCode_value);
			json.put(Protocols.SUBCODE, Protocols.G2l_playerLogout.subCode_value);
			json.put(Protocols.G2l_playerLogout.UUID, player.getUuid());
			msg.setJson(json);
			LoginMsgSender.getInstance().addMsg(msg);
		}
		return player;
	}
	
	public void playerLogout(Player player){
		logoutCache.put(player.getUuid(), player, loginCacheTime);
	}
	
	public Player getLogoutPlayer(String uuid){
		return logoutCache.getV(uuid);
	}
	
	
	
}
