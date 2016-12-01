package com.gz.gamecity.gameserver;

import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.handler.impl.LoginServerMsgHandler;
import com.gz.gamecity.gameserver.handler.impl.PlayerMsgHandler;
import com.gz.gamecity.gameserver.service.common.LoginServerService;
import com.gz.gamecity.gameserver.service.common.PlayerLoginService;
import com.gz.gamecity.gameserver.service.common.PlayerVerifyService;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.Config;
import com.gz.websocket.protocol.client.ProtocolClient;
import com.gz.websocket.server.WebSocketServer;

public class GameServiceMain {
	
	private static GameServiceMain instance;
	
	/**
	 * 表示是否连接上登录服务器
	 */
	private boolean connected=false;
	
	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public static synchronized GameServiceMain getInstance() {
		if (instance == null)
			instance = new GameServiceMain();
		return instance;
	}

	private GameServiceMain() {

	}

	public void startServer() {
		loadConfig();
		initDB();
		startLogic();
		initWebsocket();
	}

	private void initWebsocket() {
		
		LoginMsgSender.getInstance().start();
		PlayerMsgSender.getInstance().start();

//		LSConnecter connecter=LSConnecter.getInstance();
//		connecter.connectLoginServer();
		
//		LoginServerMsgHandler handler = new LoginServerMsgHandler();
//		final ProtocolClient client = new ProtocolClient(Config.instance().getSValue(ConfigField.LOGINSERVER_HOST),
//				Config.instance().getIValue(ConfigField.LOGINSERVER_PORT), handler);
		Thread t1 = new Thread() {
			@Override
			public void run() {
				try {
//					client.run();
					LSConnecter.getInstance().connectLoginServer();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t1.start();
		
		final WebSocketServer webSocketServer=new WebSocketServer(new PlayerMsgHandler());
		Thread t2 = new Thread() {
			@Override
			public void run() {
				try {
					webSocketServer.run(Config.instance().getIValue(ConfigField.WEBSOCKET_PORT));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t2.start();
		
	}

	private void startLogic() {
		PlayerManager.getInstance();
		 GSMsgReceiver.getInstance().registHandler(new LoginServerService());
		 GSMsgReceiver.getInstance().registHandler(PlayerLoginService.getInstance());
		 GSMsgReceiver.getInstance().registHandler(new PlayerVerifyService());
		// PlayerLoginService.getInstance());
		GSMsgReceiver.getInstance().start();
	}

	private void initDB() {
		// TODO Auto-generated method stub

	}

	private void loadConfig() {
		Config.instance().init();

	}

	public void stopServer() {
		// TODO
	}
	
}
