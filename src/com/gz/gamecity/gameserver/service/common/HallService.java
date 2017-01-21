package com.gz.gamecity.gameserver.service.common;


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
import com.gz.util.DateUtil;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

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
		case Protocols.C2g_signin.subCode_value:
			handleSignin(player,msg);
			break;
		case Protocols.C2g_req_signin_reward.subCode_value:
			handleReqSigninReward(player,msg);
			break;
		case Protocols.C2g_buy_head.subCode_value:
			handleBuyHead(player,msg);
			break;
		default:
			break;
		}
	}

	private void handleBuyHead(Player player, ClientMsg msg) {
		msg.put(Protocols.SUBCODE, Protocols.G2c_buy_head.subCode_value);
		JSONArray arr = AllTemplate.getHeads_config();
		int id = msg.getJson().getIntValue(Protocols.C2g_buy_head.ID);
		if(id<1 || id>16)
			return;
		for(int i=0;i<player.getHeads().length;i++){
			if(player.getHeads()[i]==id){
				msg.put(Protocols.ERRORCODE, "已经拥有该头像了");
				PlayerMsgSender.getInstance().addMsg(msg);
				return;
			}
		}
		JSONObject head_info = null;
		for(int i=0;i<arr.size();i++){
			JSONObject par = arr.getJSONObject(i);
			if(par.getInteger("id")==id){
				head_info = par;
				break;
			}
		}
		
		if(head_info.getInteger("prize")>player.getCoin()){
			msg.put(Protocols.ERRORCODE, "游戏币不足");
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		PlayerMsgSender.getInstance().addMsg(msg);
		
		PlayerDataService.getInstance().modifyCoin(player, -head_info.getInteger("prize"), EventLogType.buy_head);
		PlayerDataService.getInstance().addHead(player, id);
		
		
	}

	private void handleReqSigninReward(Player player, ClientMsg msg) {
		msg.put(Protocols.SUBCODE, Protocols.G2c_req_signin_reward.subCode_value);
		msg.put(Protocols.G2c_req_signin_reward.REWARD_INFO, AllTemplate.getSign_config());
		PlayerMsgSender.getInstance().addMsg(msg);
	}

	private void handleSignin(Player player, ClientMsg msg) {
		msg.put(Protocols.SUBCODE, Protocols.G2c_signin.subCode_value);
		if(player.isSigned()){
			msg.put(Protocols.ERRORCODE, "今天已经签到过了");
			PlayerMsgSender.getInstance().addMsg(msg);
			return;
		}
		JSONArray reward = null;
		JSONArray sign = AllTemplate.getSign_config();
		for (int i = 0; i < sign.size(); i++) {
			int vip = sign.getJSONObject(i).getIntValue("vip");
			if(vip == player.getVip())
			{
				reward = sign.getJSONObject(i).getJSONArray("reward");
				break;
			}
		}
		int days = player.getSignDays();
		String lastday = player.getLastSignDate();
		long date_diff = 0;
		long coin_reward;
		if(lastday!=null){
			try {
				date_diff = DateUtil.dateDays(lastday,DateUtil.getCurDateTime("yyyy-MM-dd"));
			} catch (Exception e) {
				days = 0;
			}
			
		}
		
		
		
		if(date_diff==1){
			days++;
			if(days>7)
				days=7;
		}else{
			days=1;
		}
		player.setSignDays(days);
		coin_reward = reward.getLongValue(days - 1);
		PlayerDataService.getInstance().modifyCoin(player, coin_reward, EventLogType.signin);

		player.setSigned(true);
		player.setLastSignDate(DateUtil.getCurDateTime("yyyy-MM-dd"));
		
		ProtocolMsg pMsg = new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_player_signin.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_player_signin.subCode_value);
		pMsg.put(Protocols.G2l_player_signin.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_player_signin.SIGN_DAYS, days);
		pMsg.put(Protocols.G2l_player_signin.DATE, player.getLastSignDate());
		LoginMsgSender.getInstance().addMsg(pMsg);
		
		PlayerDataService.getInstance().refreshPlayerData(player);
		PlayerMsgSender.getInstance().addMsg(msg);
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
