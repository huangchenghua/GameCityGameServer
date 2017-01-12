package com.gz.gamecity.gameserver.service.common;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class PlayerDataLoginService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg bMsg) {
		ProtocolMsg msg = (ProtocolMsg)bMsg;
		int subCode = msg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.L2g_mail_list.subCode_value:
			handleMailList(msg);
			break;
		
		default:
			break;
		}
	}

	private void handleMailList(ProtocolMsg msg) {
		String uuid = msg.getJson().getString(Protocols.L2g_mail_list.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player==null || !player.isOnline()){
			return;
		}
		ClientMsg cMsg = new ClientMsg();
		cMsg.setJson(msg.getJson());
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_mail_list.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_mail_list.subCode_value);
		cMsg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYER_DATA_LOGIN;
	}

}
