package com.gz.gamecity.gameserver.service.common;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class PlayerVerifyService implements LogicHandler {

	@Override
	public void handleMsg(BaseMsg msg) {
		ProtocolMsg pMsg=(ProtocolMsg)msg;
		int subCode = pMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.L2g_playerVerify.subCode_value:
			ClientMsg cMsg=new ClientMsg();
			cMsg.getJson().put(Protocols.MAINCODE, Protocols.G2c_login.mainCode_value);
			cMsg.getJson().put(Protocols.SUBCODE, Protocols.G2c_login.subCode_value);
			String uuid = pMsg.getJson().getString(Protocols.L2g_playerVerify.UUID);
//			Player player=PlayerLoginService.getInstance().getOnlinePlayer(uuid);
			Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
			if(player!=null){
				String err = pMsg.getJson().getString(Protocols.ERRORCODE);
				if(err!=null && !err.equals("")){ //验证失败
					cMsg.getJson().put(Protocols.ERRORCODE, "验证失败");
					player.write(cMsg.getJson()); //这里就直接返回失败然后关闭连接，如果塞入发送队列，就无法关闭连接
					
				}
				else{
					cMsg.setChannel(player.getChannel());
					cMsg.getJson().put(Protocols.G2c_login.NAME, pMsg.getJson().get(Protocols.L2g_playerVerify.NAME));
					cMsg.getJson().put(Protocols.G2c_login.COIN, pMsg.getJson().get(Protocols.L2g_playerVerify.COIN));
					PlayerMsgSender.getInstance().addMsg(cMsg);
					
				}
			}
		
			
			break;
		}

	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYERVERIFY;
	}

}
