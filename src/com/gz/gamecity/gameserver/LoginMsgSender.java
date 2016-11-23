package com.gz.gamecity.gameserver;

import java.util.concurrent.LinkedBlockingQueue;

import com.gz.websocket.msg.BaseMsg;
import com.gz.websocket.msg.ProtocolMsg;

public class LoginMsgSender extends Thread {
	private static LoginMsgSender instance;

	public static synchronized LoginMsgSender getInstance() {
		if(instance==null) instance=new LoginMsgSender();
		return instance;
	}
	private LoginMsgSender(){
		
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
				// TODO 这里如果发送异常就要尝试与登陆服重连
			}
		}
	}
}
