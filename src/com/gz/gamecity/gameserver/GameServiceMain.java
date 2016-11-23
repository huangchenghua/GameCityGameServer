package com.gz.gamecity.gameserver;

import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.handler.impl.LoginServerMsgHandler;
import com.gz.util.Config;
import com.gz.websocket.protocol.client.ProtocolClient;

public class GameServiceMain {
	private static GameServiceMain instance;
	
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
		
		LoginServerMsgHandler handler = new LoginServerMsgHandler();
		final ProtocolClient client = new ProtocolClient(Config.instance().getSValue(ConfigField.LOGINSERVER_HOST),
				Config.instance().getIValue(ConfigField.LOGINSERVER_PORT), handler);
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					client.run();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	private void startLogic() {
		// GSMsgReceiver.getInstance().registHandler(ProtocolsField.C2L_LOGIN.mainCode_value,
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
