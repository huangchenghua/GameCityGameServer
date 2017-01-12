package com.gz.gamecity.gameserver.service.niuniu;

import java.util.Comparator;

import com.alibaba.fastjson.JSONObject;
import com.gz.gamecity.gameserver.service.niuniu.Const.GameCardType;
import com.gz.gamecity.protocol.Protocols;

public class NiuniuPoker {
	
	/**
	 * 5张手牌，数字类型，未排序的
	 */
	private int[] cards;
	

	/**
	 * 基本牌型，0位表示类型，后5位数字类型已排序
	 */
	private int[] baseType;
	/**
	 * 牛牛牌型，0表示牛牛的牌型类型，后5位数字按照牛牛排序
	 */
	public int[] cardsData;
	
	/**
	 * 点数最大的一张牌
	 */
	private int maxPoint=-1;
	
	public Const.GameCardType cardType;
	
	/**
	 * 牌在谁的手上
	 */
	public int index_table_poker;
	
	/**
	 * 在牌桌上的大小顺序,index越小，牌越大
	 */
	public int index_rank;
	
	

	public NiuniuPoker(int[] cards) {
		//this.cards = cards;
		
		//找点数最大的那张手牌
		int[] card_point = new int[5];  
		System.arraycopy(cards, 0, card_point, 0, cards.length);
		for(int i=0;i<card_point.length;i++){
			//48-51是A，因为A当1用，所以要减掉48
			if(card_point[i]>=48 && card_point[i]<=51){
				card_point[i] = card_point[i]-48;
			}
			else
				card_point[i] = card_point[i]+4;
			if(card_point[i]>maxPoint)
				maxPoint = card_point[i];
		}
		
		this.baseType = PokerCommon.getRankedCardBaseType(Const.GameType.DOUNIU, cards);
		int[] tmp= new int[baseType.length];
		System.arraycopy(baseType, 0, tmp, 0, baseType.length);
		this.cardsData = PokerCommon.checkDOUNIUType(tmp);
		this.cards = new int[5];
		System.arraycopy(cardsData, 1, this.cards, 0, this.cards.length);
		this.cardType = Const.GameCardType.nameOfValue(Const.GameType.DOUNIU, cardsData[0]);
	}

	public static Comparator<NiuniuPoker> comparator=new Comparator<NiuniuPoker>() {
		
		@Override
		public int compare(NiuniuPoker poker1, NiuniuPoker poker2) {
			Const.GameCardType cardType1 = poker1.cardType;
			Const.GameCardType cardType2 = poker2.cardType;
			//先比较牌型，如果牌型相同再做特殊比较
			if(cardType1.value()>cardType2.value())
				return -1;
			else if(cardType1.value()<cardType2.value())
				return 1;
			else{
				if(cardType1 == GameCardType.DN_BOMB || cardType1 == GameCardType.DN_TRANGLE_DOUBLE || cardType1 == GameCardType.DN_TRANGLE_SINGLE){//四条和三条比较主要部分
					if(poker1.cardsData[1]>=48 && poker1.cardsData[1]<=51){ //A算最小的，所以主要部分有A肯定算小
						return 1;
					}
					if(poker2.cardsData[1]>=48 && poker2.cardsData[1]<=51){
						return -1;
					}
					if(poker1.cardsData[1]>poker2.cardsData[1])
						return -1;
					else
						return 1;
				}else{
					if(poker1.maxPoint > poker2.maxPoint)
						return -1;
					else
						return 1;
				}
			}
		}
	};
	
	public String toString() {
		String card_type=Const.GameCardType.nameOfValue(Const.GameType.DOUNIU, cardsData[0]).toString();
		int[] tmp=new int[5];
		System.arraycopy(cardsData, 1, tmp, 0, 5);
		return card_type + ":"+printCards(tmp)+"   "+Utils.printArray(cards, cards.length);
	};
	
	public static String printCards(int[] cards){
		int count = cards == null ? 0 : cards.length;
		if(count == 0) return null;
		StringBuffer sb=new StringBuffer("");
		for(int cardId : cards){
			sb.append(PokerCommon.getCardNameByID(cardId) + "\t");
		}
		return sb.toString();
	}
	
	public int[] getCards() {
		return cards;
	}
	
	public JSONObject toJsonObject(){
		JSONObject json=new JSONObject();
		json.put(Protocols.G2c_niuniu_result.ResultInfo.ID, index_table_poker);
		json.put(Protocols.G2c_niuniu_result.ResultInfo.INDEX_RANK, index_rank);
		json.put(Protocols.G2c_niuniu_result.ResultInfo.CARDTYPE, cardType.value());
		json.put(Protocols.G2c_niuniu_result.ResultInfo.POKER, cards);
		return json;
	}
}

