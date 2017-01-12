package com.gz.gamecity.gameserver.room;

public enum RoomType {
	Laba(1,"拉霸"),Fruit(2,"水果机"),LuckyWheel(3,"幸运转盘"),BlackA(4,"黑桃A"),Niuniu(5,"万人牛牛"),Mahjong(6,"血战麻将"),Texas(7, "德州扑克");
	
	private int roomId;
	private String name;
	
	
	public int getRoomId() {
		return roomId;
	}
	public String getName() {
		return name;
	}
	private RoomType(int id,String name){
		this.roomId=id;
		this.name = name;
	}
	public static RoomType getRoomType(int roomId){
		if(roomId == Laba.roomId){
			return Laba;
		}else if (roomId == Fruit.roomId) {
			return Fruit;
		}else if(roomId==LuckyWheel.roomId){
			return LuckyWheel;
		}else if(roomId==BlackA.roomId){
			return BlackA;
		}else if(roomId==Niuniu.roomId){
			return Niuniu;
		}else if(roomId==Mahjong.roomId){
			return Mahjong;
		}else if (roomId == Texas.roomId) {
			return Texas;
		}
		
		return null;
	}
}
