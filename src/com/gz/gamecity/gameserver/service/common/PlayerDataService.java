package com.gz.gamecity.gameserver.service.common;

import java.util.UUID;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.bean.EventLogType;
import com.gz.gamecity.bean.Player;
import com.gz.gamecity.gameserver.LoginMsgSender;
import com.gz.gamecity.gameserver.PlayerManager;
import com.gz.gamecity.gameserver.PlayerMsgSender;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.service.LogicHandler;
import com.gz.gamecity.gameserver.service.db.DBService;
import com.gz.gamecity.protocol.Protocols;
import com.gz.gamecity.protocol.Protocols.G2c_player_refresh;
import com.gz.util.Config;
import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ClientMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class PlayerDataService implements LogicHandler {

	private static PlayerDataService instance;
	
	
	
	public static synchronized PlayerDataService getInstance() {
		if(instance==null)
			instance=new PlayerDataService();
		return instance;
	}

	private PlayerDataService(){
		
	}
	
	@Override
	public void handleMsg(BaseMsg bMsg) {
		ClientMsg msg =(ClientMsg)bMsg;
		Player player=PlayerManager.getPlayerFromMsg(msg);
		if(player==null){
			msg.getChannel().close();
			return;
		}
		int subCode = msg.getJson().getIntValue(Protocols.SUBCODE);
		switch (subCode) {
		case Protocols.C2g_data_change.subCode_value:
			handleDataChange(player,msg);
			break;
		case Protocols.C2g_player_gift_list.subCode_value:
			handleGiftList(player);
			break;
		default:
			break;
		}

	}

	private void handleGiftList(Player player) {
		ProtocolMsg msg = new ProtocolMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2l_player_gift_list.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2l_player_gift_list.subCode_value);
		msg.put(Protocols.G2l_player_gift_list.UUID, player.getUuid());
		LoginMsgSender.getInstance().addMsg(msg);
	}

	private void handleDataChange(Player player, ClientMsg msg) {
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		String name = msg.getJson().getString(Protocols.C2g_data_change.NAME);
		msg.put(Protocols.SUBCODE, Protocols.G2c_data_change.subCode_value);
		if(name!=null){
			if(!checkName(name)){
				msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str11"));
			}else
			{
				player.setName(name);
				pMsg.put(Protocols.G2l_data_change.NAME, name);
			}
		}
		String sign = msg.getJson().getString(Protocols.C2g_data_change.SIGN);
		if(sign!=null){
			if(!checkSign(sign)){
				msg.put(Protocols.ERRORCODE, AllTemplate.getGameString("str11"));
			}else
			{
				player.setSign(sign);
				pMsg.put(Protocols.G2l_data_change.SIGN, sign);
			}
		}
		String head_str = msg.getJson().getString(Protocols.C2g_data_change.HEAD);
		if(head_str!=null){
			int head = Integer.parseInt(head_str);
			if(!checkHead(head)){
				msg.getChannel().close();
				return;
			}else{
				player.setHead(head);
				pMsg.put(Protocols.G2l_data_change.HEAD, head);
			}
		}
		String sex_str = msg.getJson().getString(Protocols.C2g_data_change.SEX);
		if(sex_str!=null){
			byte sex = Byte.parseByte(sex_str);
			if(!checkSex(sex)){
				msg.getChannel().close();
				return;
			}else{
				player.setSex(sex);
				pMsg.put(Protocols.G2l_data_change.SEX, sex);
			}
		}
		PlayerMsgSender.getInstance().addMsg(msg);
		refreshPlayerData(player);
		
		LoginMsgSender.getInstance().addMsg(pMsg);
	}
	
	public void refreshPlayerData(Player player){
		ClientMsg msg_data=new ClientMsg();
		msg_data.setJson(getPlayerDataJson(player));
		msg_data.put(Protocols.MAINCODE, Protocols.G2c_player_refresh.mainCode_value);
		msg_data.put(Protocols.SUBCODE, Protocols.G2c_player_refresh.subCode_value);
		msg_data.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg_data);
	}

	private boolean checkSex(byte sex) {
		if(sex<0||sex>2)
			return false;
		return true;
	}

	private boolean checkHead(int head) {
		if(head<1||head>16)
			return false;
		return true;
	}

	private boolean checkName(String name) {
		if(name.length()>8||name.length()<1)
			return false;
		return true;
	}

	private boolean checkSign(String sign) {
		if(sign.length()>30)
			return false;
		return true;
	}

	@Override
	public int getMainCode() {
		return Protocols.MainCode.PLAYER_DATA_GAME;
	}

//	public void modifyCoin(Player player,long change){
//		modifyCoin(player,change,EventLogType.unknown);
//	}
	
	public void modifyCoin(Player player,long change,EventLogType type){
		long result = player.getCoin()+change;
		if(result<0){
			player.setCoin(0);
		}else{
			if(result>Config.instance().getLValue(ConfigField.MAX_COIN))
				result = Config.instance().getLValue(ConfigField.MAX_COIN);
			player.setCoin(result);
		}
		
		ClientMsg msg =new ClientMsg();
		msg.put(Protocols.MAINCODE, Protocols.G2c_coinChange.mainCode_value);
		msg.put(Protocols.SUBCODE, Protocols.G2c_coinChange.subCode_value);
		msg.put(Protocols.G2c_coinChange.COIN, player.getCoin());
		msg.put(Protocols.G2c_coinChange.CHANGE, change);
		msg.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg);
		
		ProtocolMsg pMsg = new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_coinChange.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_coinChange.subCode_value);
		pMsg.put(Protocols.G2l_coinChange.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_coinChange.COIN, player.getCoin());
		pMsg.put(Protocols.G2l_coinChange.CHANGE, change);
		pMsg.put(Protocols.G2l_coinChange.TYPE, type.getType());
		LoginMsgSender.getInstance().addMsg(pMsg);
		
		JSONObject j =new JSONObject();
		j.put(Protocols.MAINCODE, Protocols.DB_game_coin_change.mainCode_value);
		j.put(Protocols.SUBCODE, Protocols.DB_game_coin_change.subCode_value);
		j.put(Protocols.DB_game_coin_change.PLAYER_UUID, player.getUuid());
		j.put(Protocols.DB_game_coin_change.LOG_UUID, UUID.randomUUID().toString());
		j.put(Protocols.DB_game_coin_change.COIN, player.getCoin());
		j.put(Protocols.DB_game_coin_change.CHANGE, change);
		j.put(Protocols.DB_game_coin_change.TYPE, type.getType());
		DBService.getInstance().addMsg(j);
	}
	
	
	private JSONObject getPlayerDataJson(Player player){
		JSONObject j = new JSONObject();
		j.put(Protocols.G2c_player_refresh.NAME, player.getName());
		j.put(Protocols.G2c_player_refresh.HEAD, player.getHead());
		j.put(Protocols.G2c_player_refresh.SEX, player.getSex());
		j.put(Protocols.G2c_player_refresh.COIN, player.getCoin());
		j.put(Protocols.G2c_player_refresh.CHARM, player.getCharm());
		j.put(Protocols.G2c_player_refresh.CHARGE_TOTAL, player.getCharge_total());
		j.put(Protocols.G2c_player_refresh.SIGN, player.getSign());
		j.put(Protocols.G2c_player_refresh.LVL, player.getLvl());
		j.put(Protocols.G2c_player_refresh.EXP, player.getExp());
		j.put(Protocols.G2c_player_refresh.VIP, player.getVip());
		j.put(Protocols.G2c_player_refresh.FINANCE, player.getFinance());
		j.put(Protocols.G2c_player_refresh.FROZEN, player.isFrozen());
		j.put(Protocols.G2c_player_refresh.SILENT, player.isSilent());
		j.put(Protocols.G2c_player_refresh.LASTSIGNDATE, player.getLastSignDate());
		j.put(Protocols.G2c_player_refresh.SIGNDAYS, player.getSignDays());
		j.put(Protocols.G2c_player_refresh.SIGNED, player.isSigned());
		j.put(Protocols.G2c_player_refresh.HEADS, player.getHeads());
		return j;
	}
	
	public void changeCharm(Player player,int change){
		player.setCharm( player.getCharm()+change);
		ClientMsg msg_data=new ClientMsg();
		msg_data.setJson(getPlayerDataJson(player));
		msg_data.put(Protocols.MAINCODE, Protocols.G2c_player_refresh.mainCode_value);
		msg_data.put(Protocols.SUBCODE, Protocols.G2c_player_refresh.subCode_value);
		msg_data.setChannel(player.getChannel());
		PlayerMsgSender.getInstance().addMsg(msg_data);
		
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_data_change.CHARM, player.getCharm());
		LoginMsgSender.getInstance().addMsg(pMsg);
	}
	
	public void addExp(Player player,int exp){
		int lvl = player.getLvl();
		player.setExp(player.getExp()+exp);
		JSONArray exp_config = AllTemplate.getExp_config();
		for (int i = 0; i < exp_config.size(); i++) {
			JSONObject j = exp_config.getJSONObject(i);
			int _lvl = j.getIntValue("lvl");
			int _exp = j.getIntValue("exp");
			if(player.getExp()>=_exp){
				if(_lvl!=lvl){
					player.setLvl(_lvl);
				}
				break;
			}
		}
		refreshPlayerData(player);
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_data_change.LVL, player.getLvl());
		pMsg.put(Protocols.G2l_data_change.EXP, player.getExp());
		LoginMsgSender.getInstance().addMsg(pMsg);
	}

	
	public void setAlms(Player player, byte nAlmsCnt, String strAlmsTime) {
		player.setAlmsCnt(nAlmsCnt);
		player.setAlmsTime(strAlmsTime);
		ProtocolMsg pMsg=new ProtocolMsg();
		
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_data_change.ALMS_CNT, player.getAlmsCnt());
		pMsg.put(Protocols.G2l_data_change.ALMS_TIME, player.getAlmsTime());
		LoginMsgSender.getInstance().addMsg(pMsg);
	}

	public void addHead(Player player, int head) {
		int[] newheads = new int[player.getHeads().length + 1];
		System.arraycopy(player.getHeads(), 0, newheads, 0, player.getHeads().length);
		newheads[newheads.length - 1] = head;
		player.setHeads(newheads);
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_data_change.HEADS, player.getHeads());
		LoginMsgSender.getInstance().addMsg(pMsg);
		refreshPlayerData(player);
	}

	public void playerCharge(Player player, long coin){
		modifyCoin(player,coin,EventLogType.charge);
		long charge_total=player.getCharge_total()+coin/Config.instance().getIValue(ConfigField.CHARGE_RATE);
		player.setCharge_total(charge_total);
		int vip=getvipLevel(charge_total);
		player.setVip(vip);
		PlayerDataService.getInstance().refreshPlayerData(player);
		
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_data_change.VIP, player.getVip());
		pMsg.put(Protocols.G2l_data_change.CHARGE_TOTAL, player.getCharge_total());
		LoginMsgSender.getInstance().addMsg(pMsg);
	}
	
	public void checkVip(Player player){
		long charge_total=player.getCharge_total();
		int vip=getvipLevel(charge_total);
		if(vip == player.getVip()) return;
		player.setVip(vip);
//		PlayerDataService.getInstance().refreshPlayerData(player);
		
		ProtocolMsg pMsg=new ProtocolMsg();
		pMsg.put(Protocols.MAINCODE, Protocols.G2l_data_change.mainCode_value);
		pMsg.put(Protocols.SUBCODE, Protocols.G2l_data_change.subCode_value);
		pMsg.put(Protocols.G2l_data_change.UUID, player.getUuid());
		pMsg.put(Protocols.G2l_data_change.VIP, player.getVip());
		pMsg.put(Protocols.G2l_data_change.CHARGE_TOTAL, player.getCharge_total());
		LoginMsgSender.getInstance().addMsg(pMsg);
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
}
