package com.gz.gamecity.gameserver.service.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
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

		default:
			break;
		}
		
	}

	private void handleTestCharge(Player player,ClientMsg msg) {
		// TODO 给玩家加钱，同时对 vip改变做处理
		int change = msg.getJson().getIntValue("coin");
		PlayerDataService.getInstance().modifyCoin(player,change,EventLogType.test);
		long charge_total=player.getCharge_total()+change/1000;
		player.setCharge_total(charge_total);
		int vip=getvipLevel(charge_total);
			player.setVip(vip);
		JSONObject data=new JSONObject();
		data.put("name", player.getName());
		data.put("head", player.getHead());
		data.put("vip", player.getVip());
		data.put("charge_total", player.getCharge_total());
		PlayerDataService.getInstance().modifyData(player, data);
	
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
