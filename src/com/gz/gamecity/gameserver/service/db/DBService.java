package com.gz.gamecity.gameserver.service.db;

import java.util.concurrent.LinkedBlockingQueue;

import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.gameserver.db.PlayerDao;
import com.gz.gamecity.protocol.Protocols;

public class DBService {
	private static DBService instance;
	
	private LinkedBlockingQueue<JSONObject> list = new LinkedBlockingQueue<>();
	private PlayerDao dao = new PlayerDao();
	public static synchronized DBService getInstance() {
		if(instance == null)
			instance = new DBService();
		return instance;
	}
	
	private DBService(){
		start();
	}
	
	public void addMsg(JSONObject j){
		try {
			list.put(j);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void start(){
		Thread t = new Thread(){
			@Override
			public void run() {
				while(true){
					try {
						JSONObject j = list.take();
						handleMsg(j);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
		};
		t.start();
	}
	
	private void handleMsg(JSONObject j){
		int subcode = j.getIntValue(Protocols.SUBCODE);
		switch (subcode) {
		case Protocols.DB_game_coin_change.subCode_value:
			String player_uuid = j.getString(Protocols.DB_game_coin_change.PLAYER_UUID);
			String log_uuid = j.getString(Protocols.DB_game_coin_change.LOG_UUID);
			long coin = j.getLongValue(Protocols.DB_game_coin_change.COIN);
			long change = j.getLongValue(Protocols.DB_game_coin_change.CHANGE);
			int type = j.getIntValue(Protocols.DB_game_coin_change.TYPE);
			dao.recordCoinChange(player_uuid, coin, change, type, log_uuid);
			break;

		default:
			break;
		}
	}
}
