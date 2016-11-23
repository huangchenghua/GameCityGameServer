package com.gz.gamecity.gameserver;

import java.util.concurrent.LinkedBlockingQueue;

import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class PlayerMsgSender extends Thread {

	private static PlayerMsgSender instance;

	public static synchronized PlayerMsgSender getInstance() {
		if(instance==null) instance=new PlayerMsgSender();
		return instance;
	}
	private PlayerMsgSender(){
		
	}
	
	private static LinkedBlockingQueue<ProtocolMsg> queue=new LinkedBlockingQueue<ProtocolMsg>();
	
	public void addMsg(ProtocolMsg msg){
		try {
			queue.put(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true){
			try {
				BaseMsg msg = queue.take();
				msg.sendSelf();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// TODO 这里如果发送异常就要与客户端断开连接
			}
		}
	}
	
	
}