package com.gz.gamecity.gameserver.service;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.GameServiceMain;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.logic.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class LoginServerService implements LogicHandler {

	
	@Override
	public void handleMsg(BaseMsg msg) {
		ProtocolMsg pMsg=(ProtocolMsg)msg;
		int subCode = pMsg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.L2g_login.subCode_value:
			String opt=pMsg.getJson().getString(Protocols.L2g_login.OPT);
			if(opt!=null && opt.equals(Protocols.ProtocolConst.L2G_LOGIN_OPT_SUC)){
				LoginMsgSender.getInstance().setChannel(msg.getChannel());
				GameServiceMain.getInstance().setConnected(true);
			}
			break;
		case Protocols.L2g_playerVerify.subCode_value:
			ClientMsg cMsg=new ClientMsg();
			cMsg.getJson().put(Protocols.MAINCODE, Protocols.G2c_login.mainCode_value);
			cMsg.getJson().put(Protocols.SUBCODE, Protocols.G2c_login.subCode_value);
			String uuid = pMsg.getJson().getString(Protocols.L2g_playerVerify.UUID);
			Player player=PlayerLoginService.getInstance().getOnlinePlayer(uuid);
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
		default:
			break;
		}
		
	}

}
