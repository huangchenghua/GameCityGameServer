package com.gz.gamecity.gameserver.service.common;

import java.util.UUID;

import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.DateUtil;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class MailService implements LogicHandler {

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
		case Protocols.C2g_mail_list.subCode_value:
			handleReqMailList(player);
			break;
		case Protocols.C2g_open_mail.subCode_value:
			handleOpenMail(player,msg);
			break;
		case Protocols.C2g_take_mail.subCode_value:
			handleTakeMail(player,msg);
			break;
		case Protocols.C2g_delete_mail.subCode_value:
			handleDelMail(player,msg);
			break;
		default:
			break;
		}
	}

	private void handleDelMail(Player player, ClientMsg msg) {
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.setJson(msg.getJson());
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_delete_mail.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_delete_mail.subCode_value);
		LoginMsgSender.getInstance().addMsg(pMsg);
	}

	private void handleTakeMail(Player player, ClientMsg msg) {
		ProtocolMsg pMsg = new ProtocolMsg();
		pMsg.setJson(msg.getJson());
		pMsg.put(Protocols.MAINCODE,Protocols.G2l_take_mail.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_take_mail.subCode_value);
		LoginMsgSender.getInstance().addMsg(pMsg);
	}

	private void handleOpenMail(Player player, ClientMsg msg) {
		ProtocolMsg pMsg = new ProtocolMsg();
		pMsg.setJson(msg.getJson());
		pMsg.put(Protocols.MAINCODE,Protocols.G2l_open_mail.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_open_mail.subCode_value);
		LoginMsgSender.getInstance().addMsg(pMsg);
	}

	private void handleReqMailList(Player player) {
		ProtocolMsg pMsg = new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE,Protocols.G2l_mail_list.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_mail_list.subCode_value);
		pMsg.put(Protocols.G2l_mail_list.UUID, player.getUuid());
		LoginMsgSender.getInstance().addMsg(pMsg);
	}

	public void sendMail(String player_uuid,String title,String content,String attachments){
		String mail_id = UUID.randomUUID().toString();
		Player player = PlayerManager.getInstance().getOnlinePlayer(player_uuid);
		if(player!=null && player.isOnline()){
			ClientMsg msg=new ClientMsg();
			msg.put(Protocols.MAINCODE, Protocols.G2c_new_mail.mainCode_value);
			msg.put(Protocols.MAINCODE, Protocols.G2c_new_mail.mainCode_value);
			msg.put(Protocols.G2c_new_mail.TITLE, title);
			msg.put(Protocols.G2c_new_mail.CONTENT, content);
			msg.put(Protocols.G2c_new_mail.MAILID, mail_id);
			msg.put(Protocols.G2c_new_mail.SEND_TIME, DateUtil.getCurDateTime());
			msg.put(Protocols.G2c_new_mail.ATTACHMENTS, attachments);
			msg.setChannel(player.getChannel());
			PlayerMsgSender.getInstance().addMsg(msg);
		}
		ProtocolMsg msg =new ProtocolMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2l_new_mail.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_new_mail.subCode_value);
		msg.put(Protocols.G2l_new_mail.TITLE, title);
		msg.put(Protocols.G2l_new_mail.CONTENT, content);
		msg.put(Protocols.G2l_new_mail.MAILID, mail_id);
		msg.put(Protocols.G2l_new_mail.SEND_TIME, DateUtil.getCurDateTime());
		msg.put(Protocols.G2l_new_mail.ATTACHMENTS, attachments);
		msg.put(Protocols.G2l_new_mail.UUID, player_uuid);
		LoginMsgSender.getInstance().addMsg(msg);
	}
	
	
	@Override
	public int getMainCode() {
		return Protocols.MainCode.MAIL;
	}

	
}
