package com.gz.gamecity.gameserver;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.gz.dbpools.ConnectionFactory;
import com.gz.gamecity.gameserver.config.AllTemplate;
import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.gamecity.gameserver.db.PlayerDao;
import com.gz.gamecity.gameserver.delay.InnerDelayManager;
import com.gz.gamecity.gameserver.handler.impl.PlayerMsgHandler;
import com.gz.gamecity.gameserver.service.common.ChatService;
import com.gz.gamecity.gameserver.service.common.HallService;
import com.gz.gamecity.gameserver.service.common.HeartService;
import com.gz.gamecity.gameserver.service.common.LoginServerService;
import com.gz.gamecity.gameserver.service.common.PlayerDataLoginService;
import com.gz.gamecity.gameserver.service.common.PlayerDataService;
import com.gz.gamecity.gameserver.service.common.PlayerLoginService;
import com.gz.gamecity.gameserver.service.common.PlayerVerifyService;
import com.gz.gamecity.gameserver.service.common.TestService;
import com.gz.gamecity.gameserver.service.db.DBService;
import com.gz.gamecity.gameserver.service.fruit.FruitService;
import com.gz.gamecity.gameserver.service.niuniu.NiuniuService;
import com.gz.gamecity.gameserver.service.single.BlackAService;
import com.gz.gamecity.gameserver.service.single.LabaService;
import com.gz.gamecity.gameserver.service.single.LuckyWheelService;
import com.gz.gamecity.gameserver.service.single.MahjongService;
import com.gz.gamecity.protocol.Protocols;
import com.gz.util.Config;
import com.gz.util.SensitivewordFilter;
import com.gz.websocket.protocol.client.ProtocolClient;
import com.gz.websocket.server.WebSocketServer;


import com.gz.gamecity.gameserver.service.texas.TexasHandler;

public class GameServiceMain {
	
	private static GameServiceMain instance;
	private static final Logger log =Logger.getLogger(GameServiceMain.class);
	
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
		InnerDelayManager.getInstance();
		GSMsgReceiver.getInstance().registHandler(new LoginServerService());
		GSMsgReceiver.getInstance().registHandler(PlayerLoginService.getInstance());
		GSMsgReceiver.getInstance().registHandler(new PlayerVerifyService());
		GSMsgReceiver.getInstance().registHandler(new LabaService());
		GSMsgReceiver.getInstance().registHandler(new LuckyWheelService());
		GSMsgReceiver.getInstance().registHandler(new BlackAService());
		GSMsgReceiver.getInstance().registHandler(new MahjongService());
		GSMsgReceiver.getInstance().registHandler(new NiuniuService());
		GSMsgReceiver.getInstance().registHandler(new TestService());
		GSMsgReceiver.getInstance().registHandler(new FruitService());
		GSMsgReceiver.getInstance().registHandler(new HeartService());
		GSMsgReceiver.getInstance().registHandler(new HallService());
		GSMsgReceiver.getInstance().registHandler(PlayerDataService.getInstance());
		GSMsgReceiver.getInstance().registHandler(new PlayerDataLoginService());
		// PlayerLoginService.getInstance());

		GSMsgReceiver.getInstance().registHandler(new ChatService());
		GSMsgReceiver.getInstance().registHandler(new TexasHandler());
		GSMsgReceiver.getInstance().start();
	}

	private void initDB() {
		try {
			ConnectionFactory.lookup("game");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		DBService.getInstance().start();
	}

	private void loadConfig() {
		SensitivewordFilter.getInstance();
		Config.instance().init();
		AllTemplate.initTemplates();
		log.info("配置文件加载成功");
	}

	public void stopServer() {
		// TODO
	}
	
}
