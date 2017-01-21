package com.gz.gamecity.gameserver;

import java.util.concurrent.ConcurrentHashMap;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.DelayCache;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

import io.netty.util.Attribute;

public class PlayerManager {
	
	private static final long loginCacheTime = 1*60*1000l;
	
	
	private static PlayerManager instance;

	private ConcurrentHashMap<String,Player> onlinePlayers;

	private DelayCache<String, Player> loginCache;

	public static synchronized PlayerManager getInstance() {
		if(instance==null)
			instance=new PlayerManager();
		return instance;
	}
	
	private PlayerManager(){
		onlinePlayers=new ConcurrentHashMap<>();
		loginCache=new DelayCache<String, Player>();
	}
	
	public void playerOnline(Player player){
		onlinePlayers.put(player.getUuid(), player);
	}
	
	public Player getOnlinePlayer(String uuid){
		return onlinePlayers.get(uuid);
	}
	
	public ConcurrentHashMap<String, Player> getOnlinePlayers() {
		return onlinePlayers;
	}
	
	public Player playerOffline(String uuid){
		Player player = onlinePlayers.remove(uuid);
		if(player!=null){
			ProtocolMsg msg=new ProtocolMsg();
			msg.put(Protocols.MAINCODE, Protocols.G2l_playerLogout.mainCode_value);
			msg.put(Protocols.SUBCODE, Protocols.G2l_playerLogout.subCode_value);
			msg.put(Protocols.G2l_playerLogout.UUID, player.getUuid());
			msg.put(Protocols.G2l_playerLogout.GAMETOKEN, player.getGameToken());
			LoginMsgSender.getInstance().addMsg(msg);
		}
		return player;
	}
	
	
	public static Player getPlayerFromMsg(BaseMsg msg){
		try {
			Attribute<Player> att= msg.getChannel().attr(Player.NETTY_CHANNEL_KEY);
			return att.get();
		} catch (Exception e) {
			return null;
		}
		
	}
	
	public void playerLogin(Player player){
		loginCache.put(player.getUuid()+player.getGameToken(), player, loginCacheTime);
	}
	
	public Player getLoginPlayer(String key){
		Player player = loginCache.getV(key);
		if(player!=null)
			loginCache.remove(key);
		return player;
	}
	
	
	public static void bindPlayer(BaseMsg msg,Player player){
		Attribute<Player> att= msg.getChannel().attr(Player.NETTY_CHANNEL_KEY);
		att.set(player);
	}
	
	
	public void sendTestPongPacket(){
		for(Player player:onlinePlayers.values()){
			if(player.isOnline()){
				ClientMsg msg =new ClientMsg();
				msg.put(Protocols.MAINCODE, Protocols.G2c_heart_pong.mainCode_value);
				msg.put(Protocols.SUBCODE, Protocols.G2c_heart_pong.subCode_value);
				msg.setChannel(player.getChannel());
				PlayerMsgSender.getInstance().addMsg(msg);
			}
		}
	}
	
	public Player playerReconnect(Player player){
		Player p = getOnlinePlayer(player.getUuid());
		if(p.isOnline()){
			ClientMsg msg = new ClientMsg();
			msg.put(Protocols.MAINCODE, Protocols.G2c_kick.mainCode_value);
			msg.put(Protocols.SUBCODE, Protocols.G2c_kick.subCode_value);
			p.write(msg.getJson());
			Attribute<Player> attr = p.getChannel().attr(Player.NETTY_CHANNEL_KEY);
			attr.set(null);
			p.getChannel().close();
			p.setChannel(player.getChannel());
			p.setGameToken(player.getGameToken());
			Attribute<Player> _attr = player.getChannel().attr(Player.NETTY_CHANNEL_KEY);
			_attr.set(p);
		}else{
			p.setOnline(true);
			p.setChannel(player.getChannel());
			p.setGameToken(player.getGameToken());
			Attribute<Player> attr = p.getChannel().attr(Player.NETTY_CHANNEL_KEY);
			attr.set(p);
		}
		
		Attribute<Player> att= p.getChannel().attr(Player.NETTY_CHANNEL_KEY);
		Player pp=att.get();
		System.out.println(pp);
		return p;
	}
}
