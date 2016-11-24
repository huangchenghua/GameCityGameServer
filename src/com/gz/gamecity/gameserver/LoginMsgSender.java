package com.gz.gamecity.gameserver;

import java.util.concurrent.LinkedBlockingQueue;

import com.gz.websocket.msg.ProtocolMsg;

import io.netty.channel.Channel;

public class LoginMsgSender extends Thread {
	private static LoginMsgSender instance;
	private Channel channel;

	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
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
			if(GameServiceMain.getInstance().isConnected()){
				try {
					ProtocolMsg msg = queue.take();
					msg.refreshContent();
					channel.writeAndFlush(msg);
				} catch (InterruptedException e) {
					e.printStackTrace();
					// TODO 这里如果发送异常就要尝试与登陆服重连
				}
			}
			else{
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
