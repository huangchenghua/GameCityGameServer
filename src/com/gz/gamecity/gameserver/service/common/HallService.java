package com.gz.gamecity.gameserver.service.common;


import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class HallService implements LogicHandler {

//	private 
	@Override
	public void handleMsg(BaseMsg bMsg) {
		ClientMsg msg = (ClientMsg)bMsg;
		Player player=PlayerManager.getPlayerFromMsg(msg);
		if(player==null){
			msg.getChannel().close();
			return;
		}
		int subCode = msg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.C2g_shop.subCode_value:
			handleShopReq(player,msg);
			break;

		default:
			break;
		}
	}

	private void handleShopReq(Player player, ClientMsg msg) {
		msg.put(Protocols.SUBCODE, Protocols.G2c_shop.subCode_value);
		msg.put(Protocols.G2c_shop.SHOPLIST, AllTemplate.getShop_config());
		PlayerMsgSender.getInstance().addMsg(msg);
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.HALL;
	}
	
	

}
