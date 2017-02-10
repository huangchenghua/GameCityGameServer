package com.gz.gamecity.gameserver.service.common;

import java.util.PropertyResourceBundle;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Mail;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;

import com.gz.gamecity.protocol.Protocols;
import com.gz.util.DateUtil;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class FriendHandler implements LogicHandler {

	private static final Logger log = Logger.getLogger(FriendHandler.class);
	
	@Override
	public void handleMsg(BaseMsg msg) {
		
		Player player = null;
		if (!msg.isInner()) {
			player = PlayerManager.getPlayerFromMsg(msg);
			if (player == null) {
				log.error("player not find, close websocket");
				msg.closeChannel();
				return;
			}
		}
		
		ClientMsg clientMsg = (ClientMsg)msg;
		
		int subCode = clientMsg.getJson().getIntValue(Protocols.SUBCODE);
		
		String strUuid = player == null ? "null" : player.getUuid();
		log.debug("handle friend [uuid=" + strUuid + " subCode=" + subCode + "]");
			
		switch (subCode) {
		case Protocols.C2g_friend_add.subCode_value :
			handleFriendAdd(player, clientMsg);
			break;
		case Protocols.C2g_friend_del.subCode_value :
			handleFriendDel(player, clientMsg);
			break;
		case Protocols.C2g_friend_list.subCode_value :
			handleFriendList(player, clientMsg);
			break;
		case Protocols.C2g_friend_other_info.subCode_value :
			handleFriendOtherInfo(player, clientMsg);
			break;
		case Protocols.C2g_senf_gift.subCode_value:
			handleSendGift(player, clientMsg);
			break;
		default :
			log.error("[friend] not find subCode: " + subCode);
		}
	}

	private void handleSendGift(Player player, ClientMsg msg) {
		msg.put(Protocols.SUBCODE, Protocols.G2c_senf_gift.subCode_value);
		String target = msg.getJson().getString(Protocols.C2g_senf_gift.TARGET);
		int id = msg.getJson().getIntValue(Protocols.C2g_senf_gift.ID);
		int count = msg.getJson().getIntValue(Protocols.C2g_senf_gift.COUNT);
		JSONArray arr = AllTemplate.getGift_config();
		JSONObject gift=null;
		for (int i = 0; i < arr.size(); i++) {
			JSONObject j = arr.getJSONObject(i);
			if(j.getIntValue("id") == id){
				gift = j;
				break;
			}
		}
		
		if(gift==null)
			return;
		
		if(count<1)return;
		int amount = gift.getIntValue("price") * count;
		if(amount>player.getCoin()){
			msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str5"));
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		PlayerMsgSender.getInstance().addMsg(msg);
		PlayerDataService.getInstance().modifyCoin(player, -amount, EventLogType.send_gift);
		
		MailService.getInstance().sendMail(target, AllTemplate.getGameString("str6"), AllTemplate.getGameString("str7")+player.getName()+AllTemplate.getGameString("str8"), id+"~"+count, Mail.MAIL_TYPE_SYSTEM,player.getUuid());
	}

	@Override
	public int getMainCode() {
		// TODO Auto-generated method stub
		return Protocols.MainCode.FRIEND;
	}
	
	private void handleFriendOtherInfo(Player player, ClientMsg clientMsg) {
		String strName = clientMsg.getJson().getString(Protocols.C2g_friend_other_info.NAME);
		if (strName == null || strName.isEmpty()) {
			log.error("handle friend other info, error client data[uuid=" + player.getUuid() + "]");
			return;
		}
		
		ProtocolMsg msg = new ProtocolMsg();
		
		msg.put(Protocols.G2l_friend_other_info.UUID_SELF, player.getUuid());
		msg.put(Protocols.G2l_friend_other_info.NAME, strName);
		
		msg.put(Protocols.MAINCODE, Protocols.G2l_friend_other_info.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_friend_other_info.subCode_value);

		LoginMsgSender.getInstance().addMsg(msg);
		
		log.debug("handle friend other info[uuid_self=" + player.getUuid() + " name=" + strName + "]");
	}

	private void handleFriendList(Player player, ClientMsg clientMsg) {
		ProtocolMsg msg = new ProtocolMsg();
		
		msg.put(Protocols.G2l_friend_list.UUID, player.getUuid());
		msg.put(Protocols.MAINCODE, Protocols.G2l_friend_list.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_friend_list.subCode_value);

		LoginMsgSender.getInstance().addMsg(msg);
		
		log.debug("handle friend list[uuid=" + player.getUuid() + "]");
	}
	
	private void handleFriendAdd(Player player, ClientMsg clientMsg) {
		String uuid = clientMsg.getJson().getString(Protocols.C2g_friend_add.UUID_OTHER);
		if ( uuid == null) {
			log.error("uuid is empty");
			return;
		}
		ProtocolMsg msg = new ProtocolMsg();
		msg.put(Protocols.G2l_friend_add.UUID_MY, player.getUuid());
		msg.put(Protocols.G2l_friend_add.UUID_OTHER, uuid);
		msg.put(Protocols.G2l_friend_add.DATE_TIME, DateUtil.getCurDateTime());
		
		msg.put(Protocols.MAINCODE, Protocols.G2l_friend_add.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_friend_add.subCode_value);

		LoginMsgSender.getInstance().addMsg(msg);
		
		log.debug("handle friend add[uuid_my=" + player.getUuid() + " uuid_other=" + uuid + "]");
	}
	
	private void handleFriendDel(Player player, ClientMsg clientMsg) {
		String uuid_other = clientMsg.getJson().getString(Protocols.C2g_friend_del.UUID_OTHER);
		if (uuid_other == null) {
			log.error("uuid_other is empty");
			return;
		}
		ProtocolMsg msg = new ProtocolMsg();
		msg.put(Protocols.G2l_friend_del.UUID, player.getUuid());
		msg.put(Protocols.G2c_friend_del.UUID_OTHER, uuid_other);
		
		msg.put(Protocols.MAINCODE, Protocols.G2l_friend_del.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_friend_del.subCode_value);
		LoginMsgSender.getInstance().addMsg(msg);
		
		log.debug("handle friend del[uuid_my=" + player.getUuid() + " uuid_other=" + uuid_other + "]");
	}
}
