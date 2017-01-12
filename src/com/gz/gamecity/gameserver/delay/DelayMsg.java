package com.gz.gamecity.gameserver.delay;

import java.util.UUID;

import com.gz.util.TimeUpItem;

public class DelayMsg extends TimeUpItem{
	protected long delayTime;
	public long getDelayTime() {
		return delayTime;
	}
	public void setDelayTime(long delayTime) {
		this.delayTime = delayTime;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	protected String id;
	public DelayMsg(long delayTime){
		this.delayTime = delayTime;
		id=UUID.randomUUID().toString();
	}
	@Override
	public void onTimeUp() {
		// TODO Auto-generated method stub
		
	}
}
