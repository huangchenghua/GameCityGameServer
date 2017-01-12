package com.gz.gamecity.gameserver.handler.impl;


import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.GSMsgReceiver;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.server.ServerMsgHandler;

import io.netty.channel.Channel;
import io.netty.util.Attribute;

public class PlayerMsgHandler implements ServerMsgHandler{

	@Override
	public void onMsgReceived(BaseMsg msg) {
		ClientMsg cMsg=new ClientMsg(msg);
		cMsg.parse();
		GSMsgReceiver.getInstance().addMsg(cMsg);
		
	}

	@Override
	public void onSessionClosed(Channel channel) {
		Attribute<Player> attr = channel.attr(Player.NETTY_CHANNEL_KEY);  
		Player player = attr.get();
		if(player!=null){
			player.setOnline(false);
			player.setChannel(null);
			ClientMsg msg=new ClientMsg();
			msg.setMainCode(Protocols.Inner_game_player_logout.mainCode_value);
			msg.put(Protocols.MAINCODE, Protocols.Inner_game_player_logout.mainCode_value);
			msg.put(Protocols.SUBCODE, Protocols.Inner_game_player_logout.subCode_value);
			msg.put(Protocols.Inner_game_player_logout.UUID, player.getUuid());
			msg.setInner(true);
			GSMsgReceiver.getInstance().addMsg(msg);
		}
	}

}
