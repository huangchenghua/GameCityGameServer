package com.gz.gamecity.gameserver.bean;


import io.netty.channel.Channel;

public class Player {
	public static final String NAME="name";
	public static final String UUID="uuid";
	public static final String COIN="coin";
	
	private String name;
	private long coin;
	private String gameToken;
	public String getGameToken() {
		return gameToken;
	}
	public void setGameToken(String gameToken) {
		this.gameToken = gameToken;
	}

	private String uuid;
	private Channel channel;
	
	
	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getCoin() {
		return coin;
	}
	public void setCoin(long coin) {
		this.coin = coin;
	}
	
	public static Player createPlayer(String uuid){
		Player player = new Player();
		player.setUuid(uuid);
		player.setCoin(50000);
		player.setName("游客");
		return player;
	}
//	public void write(JSONObject json){
//		PlayerMsgSender.getInstance()
//		channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(json)));
//	}
}
