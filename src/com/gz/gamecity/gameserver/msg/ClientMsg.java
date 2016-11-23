package com.gz.gamecity.gameserver.msg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gz.websocket.msg.BaseMsg;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class ClientMsg extends BaseMsg {
	private JSONObject json;
	
	
	public JSONObject getJson() {
		return json;
	}


	public void setJson(JSONObject json) {
		this.json = json;
	}


	public void parse(){
		json = JSONObject.parseObject(content);
		mainCode = json.getIntValue("mainCode");
	}
	
	public ClientMsg(BaseMsg msg){
		this.channel =msg.getChannel();
		this.content = msg.getContent();
		
	}
	
	@Override
	public void clear() {
		super.clear();
		json = null;
	}
	
	public void sendSelf(){
		channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(json)));
	}
}
