package com.gz.gamecity.gameserver;

import org.apache.log4j.Logger;

import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.handler.impl.LoginServerMsgHandler;
import com.gz.util.Config;
import com.gz.websocket.protocol.client.ProtocolClient;

public class LSConnecter {
	
	private static LSConnecter instance;
	private ProtocolClient client;
	private static final Logger log=Logger.getLogger(LSConnecter.class);
	public static synchronized LSConnecter getInstance() {
		if(instance==null)
			instance=new LSConnecter();
		return instance;
	}
	private LSConnecter(){
		client = new ProtocolClient(Config.instance().getSValue(ConfigField.LOGINSERVER_HOST),
				Config.instance().getIValue(ConfigField.LOGINSERVER_PORT), new LoginServerMsgHandler());
	}
	
	public void connectLoginServer(){
		try {
			client.run();
		} catch (Exception e) {
			e.printStackTrace();
			reconnect();
		}
	}
	
	public boolean reconnect(){
		try {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
			}
			log.info("开始重新连接服务器");
			client.run();
			return true;
		} catch (Exception e) {
			reconnect();
		}
		return false;
	}
	
}
