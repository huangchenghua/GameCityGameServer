package com.gz.gamecity.gameserver.service.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;

import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class PlayerDataLoginService implements LogicHandler {
	private static final Logger log = Logger.getLogger(PlayerDataLoginService.class);
	
	@Override
	public void handleMsg(BaseMsg bMsg) {
		ProtocolMsg msg = (ProtocolMsg)bMsg;
		int subCode = msg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.L2g_mail_list.subCode_value:
			handleMailList(msg);
			break;
		case Protocols.L2g_player_gift_list.subCode_value:
			handlePlayerGiftList(msg);
			break;
		case Protocols.L2g_take_mail.subCode_value:
			handleTakeMail(msg);
			break;

		case Protocols.L2g_silent.subCode_value:
			handleSilent(msg);
			break;

		case Protocols.L2g_friend_add.subCode_value:
			handleFriendAdd(msg);
			break;
			
		case Protocols.L2g_friend_del.subCode_value:
			handleFriendDel(msg);
			break;
		
		case Protocols.L2g_friend_list.subCode_value :
			handleFriendList(msg);
			break;
	
		case Protocols.L2g_friend_other_info.subCode_value :
			handleFriendOtherInfo(msg);
			break;
			
		case Protocols.L2g_gm_add_coin.subCode_value:
			handleGmAddCoin(msg);
			break;
		case Protocols.L2g_unsilent.subCode_value:
			handleUnsilent(msg);
			break;
		case Protocols.L2g_sendMail.subCode_value:
			handleSengMail(msg);
			break;
		case Protocols.L2g_sendGameNotice.subCode_value:
			handleSendGameNotice(msg);
			break;

		case Protocols.L2g_charts_get_list.subCode_value:
			handleChartsGetList(msg);
			break;

		case Protocols.L2g_player_charge.subCode_value:
			handlePlayerCharge(msg);
			break;

		default:

			break;
		}
	}
	
	private void handlePlayerCharge(ProtocolMsg msg) {
		String uuid=msg.getJson().getString(Protocols.L2g_player_charge.UUID);
		long coin = msg.getJson().getLongValue(Protocols.L2g_player_charge.COIN);
		if(uuid==null)
			return;
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player == null)
			return;
		PlayerDataService.getInstance().playerCharge(player, coin);
	}

	private void handleSendGameNotice(ProtocolMsg msg) {
		ChatService.getInstance().sendGameMsg(msg.getJson().getString(Protocols.L2g_sendGameNotice.CONTENT));
	}

	private void handleSengMail(ProtocolMsg msg) {
		String player_uuid = msg.getJson().getString(Protocols.L2g_sendMail.UUID);
		String title = msg.getJson().getString(Protocols.L2g_sendMail.TITLE);
		String content = msg.getJson().getString(Protocols.L2g_sendMail.CONTENT);
		String attachments = msg.getJson().getString(Protocols.L2g_sendMail.ATTACHMENTS);
		MailService.getInstance().sendMail(player_uuid, title, content, attachments);
	}

	private void handleUnsilent(ProtocolMsg msg) {
		String uuid = msg.getJson().getString(Protocols.L2g_unsilent.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player!=null){
			player.setSilent(false);
			PlayerDataService.getInstance().refreshPlayerData(player);
		}
	}

	private static Comparator<JSONObject> compFriendList = new Comparator<JSONObject>() {
		@Override
		public int compare(JSONObject jo1, JSONObject jo2) {
			int nCoin1 = jo1.getIntValue(Protocols.G2c_friend_list.Player_list.COIN);
			int nCoin2 = jo2.getIntValue(Protocols.G2c_friend_list.Player_list.COIN);
			if (nCoin1 > nCoin2) {
				return -1;
			} else if (nCoin1 < nCoin2) {
				return 1;
			} else {
				int nVip1 = jo1.getIntValue(Protocols.G2c_friend_list.Player_list.VIP);
				int nVip2 = jo2.getIntValue(Protocols.G2c_friend_list.Player_list.VIP);
				if (nVip1 > nVip2) {
					return -1;
				} else if (nVip1 < nVip2) {
					return 1;
				} else {
					int nLv1 = jo1.getIntValue(Protocols.G2c_friend_list.Player_list.LV);
					int nLv2 = jo2.getIntValue(Protocols.G2c_friend_list.Player_list.LV);
					if (nLv1 >= nLv1) {
						return -1;
					} else {
						return 1;
					}
				}
			}
		}
	};
	
	private void handleGmAddCoin(ProtocolMsg msg) {
		String uuid = msg.getJson().getString(Protocols.L2g_gm_add_coin.UUID);
		long coin = msg.getJson().getLongValue(Protocols.L2g_gm_add_coin.COIN);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player == null) return;
		PlayerDataService.getInstance().modifyCoin(player, coin, EventLogType.gm_add);
	}

	private void handleFriendOtherInfo(ProtocolMsg msg) {
		String strUuidSelf = msg.getJson().getString(Protocols.L2g_friend_other_info.UUID_SELF);
		Player player = PlayerManager.getInstance().getOnlinePlayer(strUuidSelf);
		if(player == null || !player.isOnline()) {
			log.debug("player is offline[uuid=" + strUuidSelf);
			return;
		}
		log.debug("friend other info[data=" + msg.getJson().toJSONString() + "]");
		
		ClientMsg cMsg =new ClientMsg();
		//String strUuid = msg.getJson().getString(Protocols.L2g_friend_other_info.UUID);
		/*
		if (strError != null) {
			cMsg.put(Protocols.ERRORCODE, "找不到该玩家");
		} else {
			JSONObject jo = msg.getJson();
			jo.remove(Protocols.L2g_friend_other_info.UUID_SELF);
			cMsg.setJson(jo);
		}*/
		
		JSONObject jo = msg.getJson();
		jo.remove(Protocols.L2g_friend_other_info.UUID_SELF);
		cMsg.setJson(jo);
	
		cMsg.setChannel(player.getChannel());
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_friend_other_info.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_friend_other_info.subCode_value);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	
	private void handleFriendList(ProtocolMsg msg) {
		String strUuid = msg.getJson().getString(Protocols.L2g_friend_list.UUID);
		
		Player player = PlayerManager.getInstance().getOnlinePlayer(strUuid);
		if(player == null || !player.isOnline()) {
			log.debug("player is offline[uuid=" + strUuid);
			return;
		}
		JSONArray ja = msg.getJson().getJSONArray(Protocols.L2g_friend_list.PLAYER_LIST);
		/*
		ArrayList<JSONObject> list = new ArrayList<JSONObject>();
		for (int i = 0; i < ja.size(); ++i) {
			list.add(ja.getJSONObject(i));
		}
		Collections.sort(list, compFriendList);
		
		JSONObject[] szJo = list.toArray(new JSONObject[list.size()]);
		*/
		JSONObject[] szJo = new JSONObject[ja.size()];
		for (int i = 0; i < ja.size(); ++i) {
			szJo[i] = ja.getJSONObject(i);
		}
		Arrays.sort(szJo, compFriendList);
		
		ClientMsg cMsg =new ClientMsg();
		//cMsg.setJson(msg.getJson());
		
		cMsg.put(Protocols.G2c_friend_list.PLAYER_LIST, szJo);
		
		cMsg.setChannel(player.getChannel());
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_friend_list.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_friend_list.subCode_value);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}

	private void handleFriendDel(ProtocolMsg msg) {
		String strUuid = msg.getJson().getString(Protocols.L2g_friend_del.UUID);
		int nRet = msg.getJson().getShort(Protocols.L2g_friend_del.RET);
		
		String strUuidOther = msg.getJson().getString(Protocols.L2g_friend_del.UUID_OTHER);
		boolean bIsSucc = nRet == 1 ? true : false;
		
		log.debug("callback friend del [uuid=" + strUuid + " uuid_other=" + strUuidOther + " ret=" + nRet + "]");
		
		Player player = PlayerManager.getInstance().getOnlinePlayer(strUuid);
		if(player == null || !player.isOnline()) {
			log.debug("player is offline[uuid=" + strUuid);
			return;
		}
		
		ClientMsg cMsg =new ClientMsg();
		cMsg.put(Protocols.G2c_friend_del.UUID_OTHER, strUuidOther);
		cMsg.put(Protocols.G2c_friend_add.IS_SUCC, bIsSucc);
		cMsg.setChannel(player.getChannel());
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_friend_del.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_friend_del.subCode_value);
		PlayerMsgSender.getInstance().addMsg(cMsg);
			
	}


	private void handleFriendAdd(ProtocolMsg msg) {
		String strUuidMy = msg.getJson().getString(Protocols.L2g_friend_add.UUID_MY);
		String strUuidOther = msg.getJson().getString(Protocols.L2g_friend_add.UUID_OTHER);
		int nRet = msg.getJson().getIntValue(Protocols.L2g_friend_add.RET);
		boolean bIsSucc = nRet == 1 ? true : false;
		
		log.debug("firend add[uuid_my=" + strUuidMy + "uuid_other=" + strUuidOther + " ret=" + nRet + "]");
		
		Player player = PlayerManager.getInstance().getOnlinePlayer(strUuidMy);
		if(player == null || !player.isOnline())
			return;
		
		ClientMsg cMsg =new ClientMsg();
		cMsg.put(Protocols.G2c_friend_add.UUID_OTHER, strUuidOther);
		cMsg.put(Protocols.G2c_friend_add.IS_SUCC, bIsSucc);
		cMsg.setChannel(player.getChannel());
		cMsg.put(Protocols.MAINCODE, Protocols.G2c_friend_add.mainCode_value);
		cMsg.put(Protocols.SUBCODE, Protocols.G2c_friend_add.subCode_value);
		PlayerMsgSender.getInstance().addMsg(cMsg);
	}
	

	private void handleSilent(ProtocolMsg msg) {
		String uuid = msg.getJson().getString(Protocols.L2g_silent.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player!=null){
			player.setSilent(true);
		}
		if(player.isOnline()){
			ClientMsg cMsg=new ClientMsg();
			cMsg.put(Protocols.MAINCODE, Protocols.G2c_player_refresh.mainCode_value);
			cMsg.put(Protocols.SUBCODE, Protocols.G2c_player_refresh.subCode_value);
			cMsg.setChannel(player.getChannel());
			cMsg.put(Protocols.G2c_player_refresh.SILENT, true);
			PlayerMsgSender.getInstance().addMsg(cMsg);
		}
	}


	private void handleTakeMail(ProtocolMsg msg) {
		JSONArray jarr = AllTemplate.getGift_config();
		String uuid = msg.getJson().getString(Protocols.L2g_take_mail.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player==null)
			return;
		String attachments = msg.getJson().getString(Protocols.L2g_take_mail.ATTACHMENTS);
		String[] _str = attachments.split(";");
		int charm=0;
		long coin=0l;
		for(int i=0;i<_str.length;i++){
			String[] par = _str[i].split("~");
			int id = Integer.parseInt(par[0]);
			int count = Integer.parseInt(par[1]);
			if(id==9){//9是发游戏币
				PlayerDataService.getInstance().modifyCoin(player, count, EventLogType.take_mail);
			}else{
				JSONObject json = jarr.getJSONObject(id-1);
				charm+=(json.getIntValue("charm")*count);
				coin+=(json.getIntValue("reward")*count);
				ClientMsg cMsg = new ClientMsg();
				cMsg.put(Protocols.MAINCODE, Protocols.G2c_player_gift_change.mainCode_value);
				cMsg.put(Protocols.SUBCODE, Protocols.G2c_player_gift_change.subCode_value);
				cMsg.put(Protocols.G2c_player_gift_change.ID, id);
				cMsg.put(Protocols.G2c_player_gift_change.COUNT, count);
				cMsg.setChannel(player.getChannel());
				PlayerMsgSender.getInstance().addMsg(cMsg);
				ProtocolMsg pMsg = new ProtocolMsg();
				pMsg.put(Protocols.MAINCODE, Protocols.G2l_player_gift_change.mainCode_value);
				pMsg.put(Protocols.SUBCODE, Protocols.G2l_player_gift_change.subCode_value);
				pMsg.put(Protocols.G2l_player_gift_change.UUID, player.getUuid());
				pMsg.put(Protocols.G2l_player_gift_change.ID, id);
				pMsg.put(Protocols.G2l_player_gift_change.COUNT, count);
				LoginMsgSender.getInstance().addMsg(pMsg);
			}
		}
		PlayerDataService.getInstance().changeCharm(player, charm);
		PlayerDataService.getInstance().modifyCoin(player, coin, EventLogType.take_mail);
	}

	private void handlePlayerGiftList(ProtocolMsg msg) {
		String uuid = msg.getJson().getString(Protocols.L2g_player_gift_list.UUID);
		Player player = PlayerManager.getInstance().getOnlinePlayer(uuid);
		if(player!=null && player.isOnline()){
			ClientMsg cMsg =new ClientMsg();
			cMsg.setJson(msg.getJson());
			cMsg.setChannel(player.getChannel());
			cMsg.put(Protocols.MAINCODE, Protocols.G2c_player_gift_list.mainCode_value);
			cMsg.put(Protocols.SUBCODE, Protocols.G2c_player_gift_list.subCode_value);
			PlayerMsgSender.getInstance().addMsg(cMsg);
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
	
	private void handleChartsGetList(ProtocolMsg msg) {
		String strUuidSelf = msg.getJson().getString(Protocols.L2g_charts_get_list.UUID_SELF);
		Player player = PlayerManager.getInstance().getOnlinePlayer(strUuidSelf);
		if (player == null || !player.isOnline()) {
			return ;
		}
		
		ClientMsg clientMsg = new ClientMsg();
		clientMsg.setJson(msg.getJson());
		clientMsg.getJson().remove(Protocols.L2g_charts_get_list.SERVER_ID);
		clientMsg.getJson().remove(Protocols.L2g_charts_get_list.UUID_SELF);
		
		clientMsg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(clientMsg);
		
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYER_DATA_LOGIN;
	}

}
