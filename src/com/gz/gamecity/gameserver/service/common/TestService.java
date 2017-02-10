package com.gz.gamecity.gameserver.service.common;

import java.util.Random;
import java.util.UUID;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Mail;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.protocol.Protocols;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;

public class TestService implements LogicHandler {

	public TestService(){
//		Thread t=new Thread(){
//			@Override
//			public void run() {
//				while(true){
//					try {
//						Thread.sleep(10*1000);
//					} catch (Exception e) {
//						// TODO: handle exception
//					}
//					PlayerManager.getInstance().sendTestPongPacket();
//				}
//			}
//		};
//		t.start();
	}
	
	@Override
	public void handleMsg(BaseMsg bMsg) {
		Player player=PlayerManager.getPlayerFromMsg(bMsg);
		ClientMsg msg=(ClientMsg) bMsg;
		int subCode = msg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.C2g_test_charge.subCode_value:
			handleTestCharge(player,msg);
			break;
		case Protocols.C2g_test_sendMail.subCode_value:
			handleSendMail(player,msg);
			break;
		default:
			break;
		}
		
	}

	private void handleSendMail(Player player,ClientMsg msg) {
		int mail_type = msg.getJson().getIntValue(Protocols.C2g_test_sendMail.MAIL_TYPE);
		if(mail_type == Mail.MAIL_TYPE_SYSTEM)
			MailService.getInstance().sendMail(player.getUuid(), "测试邮件"+new Random().nextInt(100), "测试内容"+UUID.randomUUID().toString(), getRandomAttachments());
		else
			MailService.getInstance().sendMail(player.getUuid(), "测试邮件 好友"+new Random().nextInt(100), "测试内容"+UUID.randomUUID().toString(), null,Mail.MAIL_TYPE_FRIEND,"");
	}

	
	private String getRandomAttachments(){
		StringBuffer sb=new StringBuffer("");
		for(int i=1;i<=2;i++){
			if(new Random().nextBoolean()){
				sb.append(i+"~"+new Random().nextInt(100)+";");
			}
		}
		if(sb.length()>0)
			sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
			
	private void handleTestCharge(Player player,ClientMsg msg) {
		// TODO 给玩家加钱，同时对 vip改变做处理
		int change = msg.getJson().getIntValue("coin");
		PlayerDataService.getInstance().playerCharge(player, change);
	}
	
	public int getvipLevel(long charge){
		//获取vip等级
		JSONArray viplevel_list=AllTemplate.getvipLevel_jsonArray();
		for(int i=0;i<viplevel_list.size()-1;i++){
			if(charge>=viplevel_list.getJSONObject(i).getIntValue("charge")&&charge<viplevel_list.getJSONObject(i+1).getIntValue("charge")){
				return viplevel_list.getJSONObject(i).getIntValue("vip");
			}else if(charge>=viplevel_list.getJSONObject(viplevel_list.size()-1).getIntValue("charge")){
				return viplevel_list.getJSONObject(viplevel_list.size()-1).getIntValue("vip");
			}
		}
		return 0;
	}
	
	@Override
	public int getMainCode() {
		return Protocols.MainCode.TEST;
	}

}
