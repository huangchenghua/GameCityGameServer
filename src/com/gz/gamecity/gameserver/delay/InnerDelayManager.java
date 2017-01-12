package com.gz.gamecity.gameserver.delay;

import com.gz.util.DelayCache;
import com.gz.util.TimeUpItem;

public class InnerDelayManager {
	private static InnerDelayManager instance;

	private DelayCache<String, TimeUpItem> delayQueue;
	
	public static synchronized InnerDelayManager getInstance() {
		if(instance == null)
			instance = new InnerDelayManager();
		return instance;
	}
	
	private InnerDelayManager(){
		delayQueue = new DelayCache<>();
	}
	
	public void addDelayItem(DelayMsg msg){
		delayQueue.put(msg.getId(), msg, msg.getDelayTime());
	}
	
	public void removeDelayItem(String strId) {
		delayQueue.remove(strId);
	}
	
}
