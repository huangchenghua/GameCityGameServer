package com.gz.gamecity.gameserver.service.niuniu;

import java.util.Arrays;

/**
 * 
 * @author boogle
 *
 */
public final class PokerCommon {

	/**
	 * 初始化一副牌（含大小王）
	 * @return 牌
	 */
	public static int[] initCards(){
		int[] cards = new int[54];
		for(int i = 0, len = cards.length;i < len;i++ ){
			cards[i] = i;
		}
		return cards;
	}
	
	/**
	 * 初始化一副牌除开大小王
	 * @return 牌
	 */
	public static int[] initCardsExceptJoker(){
		int[] cards = new int[52];
		for(int i = 0, len = cards.length;i < len;i++ ){
			cards[i] = i;
		}
		return cards;
	}
	
	
	/**
	 * 洗牌
	 * @param cards 牌原顺序
	 * @return 洗后的牌
	 */
	public static int[] shuffle(int[] cards){
		if(null != cards){
			synchronized(cards){
				int temp = 0;
				int randIdx = 0;
				for(int count = cards.length - 1; count > 2; count--){
					randIdx = Utils.getRandomInt(count);
					temp = cards[randIdx];
					cards[randIdx] = cards[count];
					cards[count] = temp;
				}
			}
		}
		return cards;
	}
	
	/**
	 * 根据基本牌型排好序的牌
	 * @param game 游戏玩法
	 * @param cards 牌
	 * @return 牌型数据 [基本牌型, cards0, cards1...cardsN] 
	 */
	public static int[] getRankedCardBaseType(Const.GameType game, int[] cards){

		int cardsLen = cards == null ? 0 : cards.length;
		if(cardsLen == 0)
			return null;
		
		int[] cardsData = new int[cardsLen + 1];
		//头位表示牌型 1~n是牌
		System.arraycopy(cards, 0, cardsData, 1, cardsLen);
		Arrays.sort(cardsData);
		
		
		//从大到小排
		int temp;
		for(int i = cardsLen / 2 - 1; i >= 0; i--){
			temp = cardsData[1 + i];
			cardsData[1 + i] = cardsData[cardsLen - i];
			cardsData[cardsLen - i] = temp;
		}
		
		//Ａ当1用
		boolean AisOne = game == Const.GameType.DOUNIU;
		if(AisOne){
			//把Ａ往后排
			while(getPointByCardID(cardsData[1]) == Const.CARD_POINT_A){
				temp = cardsData[1];
				for(int i = 1; i < cardsLen; i++){
					cardsData[i] = cardsData[i + 1];
				}
				cardsData[cardsLen] = temp;
			};
		}
		
		int type = 0;
		int point1 = getPointByCardID(cardsData[1]);
		int point2 = cardsLen >= 2 ? getPointByCardID(cardsData[2]) : 0;
		int point3 = cardsLen >= 3 ? getPointByCardID(cardsData[3]) : 0;
		int point4 = cardsLen >= 4 ? getPointByCardID(cardsData[4]) : 0;
		int point5 = cardsLen >= 5 ? getPointByCardID(cardsData[5]) : 0;
		
		if(AisOne){
			if(point1 == Const.CARD_POINT_A) point1 = Const.CARD_POINT_A1;
			if(point2 == Const.CARD_POINT_A) point2 = Const.CARD_POINT_A1;
			if(point3 == Const.CARD_POINT_A) point3 = Const.CARD_POINT_A1;
			if(point4 == Const.CARD_POINT_A) point4 = Const.CARD_POINT_A1;
			if(point5 == Const.CARD_POINT_A) point5 = Const.CARD_POINT_A1;
		}
		
		//是否有炸 
		if(cardsLen >= 4){
			if(point1 == point4){//KKKK, KKKK5
				type = Const.CardBaseType.BOMB.value();
			} else if(cardsLen == 5 && point2 == point5){
				//K5555  => 5555K
				type = Const.CardBaseType.BOMB.value();
				temp = cardsData[1];
				cardsData[1] = cardsData[2];
				cardsData[2] = cardsData[3];
				cardsData[3] = cardsData[4];
				cardsData[4] = cardsData[5];
				cardsData[5] = temp;
			}
		}
		
		//是否有三张
		if(type == 0 && cardsLen >= 3){
			if(point1 == point3){//KKK, KKK53 , KKK55
				if(cardsLen >= 5 && point4 == point5)
					type = Const.CardBaseType.TRANGLE_DOUBLE.value();
				else 
					type = Const.CardBaseType.TRANGLE_SINGLE.value();
				
			} else if(cardsLen > 3 && point2 == point4){//K555, K5553
				type = Const.CardBaseType.TRANGLE_SINGLE.value();
				temp = cardsData[1];
				cardsData[1] = cardsData[2];
				cardsData[2] = cardsData[3];
				cardsData[3] = cardsData[4];
				cardsData[4] = temp;
			} else if(cardsLen >= 5 && point3 == point5){//KX555
				type = Const.CardBaseType.TRANGLE_SINGLE.value();
				temp = cardsData[1];
				int temp2 = cardsData[2];
				cardsData[1] = cardsData[3];
				cardsData[2] = cardsData[4];
				cardsData[3] = cardsData[5];
				cardsData[4] = temp;
				cardsData[5] = temp2;
				
				if(point1 == point2){
					type = Const.CardBaseType.TRANGLE_DOUBLE.value();
				}
			}
		}
		
		
		//是否有双对子 
		if(type == 0 && cardsLen >= 4){
				if(point1 == point2 && point3 == point4){ //KK55 KK553
					type = Const.CardBaseType.DOUBLE_DOUBLE.value();
				} else if(cardsLen >= 5){
					if(point1 == point2 && point4 == point5){ //KK533
						type = Const.CardBaseType.DOUBLE_DOUBLE.value();
						temp = cardsData[3];
						cardsData[3] = cardsData[4];
						cardsData[4] = cardsData[5];
						cardsData[5] = temp;
					} else if(point2 == point3 && point4 == point5){//K5533
						type = Const.CardBaseType.DOUBLE_DOUBLE.value();
						temp = cardsData[1];
						cardsData[1] = cardsData[2];
						cardsData[2] = cardsData[3];
						cardsData[3] = cardsData[4];
						cardsData[4] = cardsData[5];
						cardsData[5] = temp;
					}
				}
		}
		
		//是否有一对
		if(type == 0 && cardsLen >= 2){
			if(point1 == point2){//KK5
				type = Const.CardBaseType.DOUBLE.value();
			} else if(cardsLen >=3){
				if(point2 == point3){//K55 
					type = Const.CardBaseType.DOUBLE.value();
					temp = cardsData[1];
					cardsData[1] = cardsData[2];
					cardsData[2] = cardsData[3];
					cardsData[3] = temp;
				} else if(cardsLen >= 4){ 
					if(point3 == point4){//KQ55, KQ553
						type = Const.CardBaseType.DOUBLE.value();
						temp = cardsData[1];
						int temp2 = cardsData[2];
						cardsData[1] = cardsData[3];
						cardsData[2] = cardsData[4];
						cardsData[3] = temp;
						cardsData[4] = temp2;
					} else if(cardsLen >= 5 && point4 == point5){//KQ533
						type = Const.CardBaseType.DOUBLE.value();
						temp = cardsData[1];
						int temp2 = cardsData[2];
						int temp3 = cardsData[3];
						cardsData[1] = cardsData[4];
						cardsData[2] = cardsData[5];
						cardsData[3] = temp;
						cardsData[4] = temp2;
						cardsData[5] = temp3;
					}
				}
			}
		}
		
		
		cardsData[0] = type == 0 ? Const.CardBaseType.SINGLE.value() : type;
		return cardsData;
	}
	
	/**
	 * 获取游戏中手牌具体牌型及数据
	 * @param game 游戏类型
	 * @param cards 手牌
	 * @return 牌型数据: [牌型, cards0, cards1...cardsN]
	 */
	public static int[] getGameCardsData(Const.GameType game, int[] cards){
		int[] cardsData = getRankedCardBaseType(game, cards);
		if(null == cardsData)
			return null;
		
		if(Const.GameType.DEZHOU == game)
			return checkDEZHOUType(cardsData);
		else if(Const.GameType.DOUNIU == game)
			return checkDOUNIUType(cardsData);
		else if(Const.GameType.JINHUA == game)
			return checkJINHUAType(cardsData);
		
		return null;
	}
	
	/**
	 * 德州牌型
	 * @param cardsData 初步整理后的牌数据
	 * @return 德州牌型
	 */
	private static int[] checkDEZHOUType(int[] cardsData){
		if(cardsData.length != 6)
			return null;
		
		Const.CardBaseType baseType = Const.CardBaseType.nameOfValue(cardsData[0]);
		cardsData[0] = Const.GameCardType.NORMAL.value();
		
		int point1 = getPointByCardID(cardsData[1]);
		int point2 = getPointByCardID(cardsData[2]);
		int point3 = getPointByCardID(cardsData[3]);
		int point4 = getPointByCardID(cardsData[4]);
		int point5 = getPointByCardID(cardsData[5]);
		Const.Suit suit1 = getSuitByCardID(cardsData[1]);
		Const.Suit suit2 = getSuitByCardID(cardsData[2]);
		Const.Suit suit3 = getSuitByCardID(cardsData[3]);
		Const.Suit suit4 = getSuitByCardID(cardsData[4]);
		Const.Suit suit5 = getSuitByCardID(cardsData[5]);
		
		if(Const.CardBaseType.SINGLE == baseType){
			//同花
			boolean isSameSuits = suit1 == suit2 && suit2 == suit3 && suit3 == suit4 && suit4 == suit5;
			//顺子
			boolean isQueue = point1 == 1 + point2
							&& point2 == 1 + point3
							&& point3 == 1 + point4
							&& point4 == 1 + point5;
			
			//特殊顺子Ａ当1用  Ａ5432
			boolean AisOne = false;//A当1用
			if(!isQueue && point1 == Const.CARD_POINT_A){
				isQueue = point2 == 5
						&& point3 == 4
						&& point4 == 3
						&& point5 == 2;
				if(isQueue){
					AisOne = true;
					int temp = cardsData[1];
					cardsData[1] = cardsData[2];
					cardsData[2] = cardsData[3];
					cardsData[3] = cardsData[4];
					cardsData[4] = cardsData[5];
					cardsData[5] = temp;//A
				}
			}
			
			if(isSameSuits && isQueue){
				if(Const.CARD_POINT_A == point1 && !AisOne)//皇家同花顺
					cardsData[0] = Const.GameCardType.DZ_ROYAL_SAME_SUITS_QUEUE.value();
				else//同花顺
					cardsData[0] = Const.GameCardType.DZ_SAME_SUITS_QUEUE.value();
			} else if(isSameSuits){//同花
				cardsData[0] = Const.GameCardType.DZ_SAME_SUITS.value();
			} else if(isQueue){//顺子
				cardsData[0] = Const.GameCardType.DZ_QUEUE.value();
			}
		} else if(Const.CardBaseType.BOMB == baseType){
			cardsData[0] = Const.GameCardType.DZ_BOMB.value();
		} else if(Const.CardBaseType.TRANGLE_DOUBLE == baseType){
			cardsData[0] = Const.GameCardType.DZ_TRANGLE_DOUBLE.value();
		} else if(Const.CardBaseType.TRANGLE_SINGLE == baseType){
			cardsData[0] = Const.GameCardType.DZ_TRANGLE_SINGLE.value();
		} else if(Const.CardBaseType.DOUBLE_DOUBLE == baseType){
			cardsData[0] = Const.GameCardType.DZ_DOUBLE_DOUBLE.value();
		} else if(Const.CardBaseType.DOUBLE == baseType){
			cardsData[0] = Const.GameCardType.DZ_DOUBLE.value();
		} else {
			cardsData[0] = Const.GameCardType.NORMAL.value();
		}
			
			
		return cardsData;
	}
	
	
	/**
	 * 斗牛牌型
	 * @param cardsData 初步整理后的牌数据
	 * @return 斗牛牌型
	 */
	public static int[] checkDOUNIUType(int[] cardsData){
		if(cardsData.length != 6)
			return null;
		
		Const.CardBaseType baseType = Const.CardBaseType.nameOfValue(cardsData[0]);
		cardsData[0] = Const.GameCardType.NORMAL.value();
		
		int point1 = getPointByCardID(cardsData[1]);
		int point2 = getPointByCardID(cardsData[2]);
		int point3 = getPointByCardID(cardsData[3]);
		int point4 = getPointByCardID(cardsData[4]);
		int point5 = getPointByCardID(cardsData[5]);
		Const.Suit suit1 = getSuitByCardID(cardsData[1]);
		Const.Suit suit2 = getSuitByCardID(cardsData[2]);
		Const.Suit suit3 = getSuitByCardID(cardsData[3]);
		Const.Suit suit4 = getSuitByCardID(cardsData[4]);
		Const.Suit suit5 = getSuitByCardID(cardsData[5]);
		
		if(point1 == Const.CARD_POINT_A) point1 = Const.CARD_POINT_A1;
		if(point2 == Const.CARD_POINT_A) point2 = Const.CARD_POINT_A1;
		if(point3 == Const.CARD_POINT_A) point3 = Const.CARD_POINT_A1;
		if(point4 == Const.CARD_POINT_A) point4 = Const.CARD_POINT_A1;
		if(point5 == Const.CARD_POINT_A) point5 = Const.CARD_POINT_A1;

		if(point1 <= 4 && point1 + point2 + point3 + point4 + point5 < 10){//五小牛
			cardsData[0] = Const.GameCardType.DN_NN_LESS_10.value();
		} else if(point5 >= 11 && point4 >= 11 && point3 >= 11 && point2 >= 11 && point1 >= 11){//五花牛
			cardsData[0] = Const.GameCardType.DN_ALL_ROYAL.value();
		} else if(Const.CardBaseType.SINGLE == baseType){
			//同花
			boolean isSameSuits = suit1 == suit2 && suit2 == suit3 && suit3 == suit4 && suit4 == suit5;
			//顺子
			boolean isQueue = point1 == 1 + point2
							&& point2 == 1 + point3
							&& point3 == 1 + point4
							&& point4 == 1 + point5;
			
			if(isSameSuits && isQueue){//同花顺
				cardsData[0] = Const.GameCardType.DN_SAME_SUITS_QUEUE.value();
			} else if(isSameSuits){//同花
				cardsData[0] = Const.GameCardType.DN_SAME_SUITS.value();
			} else if(isQueue){//顺子
				cardsData[0] = Const.GameCardType.DN_QUEUE.value();
			} else {
				checkNiu(cardsData);
			}
		} else if(Const.CardBaseType.BOMB == baseType){
			cardsData[0] = Const.GameCardType.DN_BOMB.value();
		} else if(Const.CardBaseType.TRANGLE_DOUBLE == baseType){
			cardsData[0] = Const.GameCardType.DN_TRANGLE_DOUBLE.value();
		} else if(Const.CardBaseType.TRANGLE_SINGLE == baseType){
			cardsData[0] = Const.GameCardType.DN_TRANGLE_SINGLE.value();
		} else if(Const.CardBaseType.DOUBLE == baseType || Const.CardBaseType.DOUBLE_DOUBLE == baseType){
			//斗牛中无 “对子” 牌型， 牌排列顺序需重排
			int cardsLen = cardsData.length - 1;
			cardsData[0] = 0;
			int[] copy = new int[cardsLen + 1];
			System.arraycopy(cardsData, 0, copy, 0, cardsLen+1);
			Arrays.sort(copy);
			cardsData = copy;
			//从大到小排
			int temp;
			for(int i = cardsLen / 2 - 1; i >= 0; i--){
				temp = cardsData[1 + i];
				cardsData[1 + i] = cardsData[cardsLen - i];
				cardsData[cardsLen - i] = temp;
			}
			
			//Ａ当1用 把Ａ往后排
			while(getPointByCardID(cardsData[1]) == Const.CARD_POINT_A){
				temp = cardsData[1];
				for(int i = 1; i < cardsLen; i++){
					cardsData[i] = cardsData[i + 1];
				}
				cardsData[cardsLen] = temp;
			};
			
			checkNiu(cardsData);
		} else {
			//无牛 牛1〜牛9
			checkNiu(cardsData);
		}
		
		
		
		return cardsData;
	}
	
	/**
	 * 查看是牛几
	 * @param cardsData 牌型数据
	 * @return 
	 */
	private static int[] checkNiu(int[] cardsData){
		
		int point1 = getPointByCardID(cardsData[1]);
		int point2 = getPointByCardID(cardsData[2]);
		int point3 = getPointByCardID(cardsData[3]);
		int point4 = getPointByCardID(cardsData[4]);
		int point5 = getPointByCardID(cardsData[5]);
		
		if(point1 == Const.CARD_POINT_A) point1 = Const.CARD_POINT_A1;
		if(point2 == Const.CARD_POINT_A) point2 = Const.CARD_POINT_A1;
		if(point3 == Const.CARD_POINT_A) point3 = Const.CARD_POINT_A1;
		if(point4 == Const.CARD_POINT_A) point4 = Const.CARD_POINT_A1;
		if(point5 == Const.CARD_POINT_A) point5 = Const.CARD_POINT_A1;
		
		int niuPoint1 = point1;
		int niuPoint2 = point2;
		int niuPoint3 = point3;
		int niuPoint4 = point4;
		int niuPoint5 = point5;

		if(niuPoint1 > 10) niuPoint1 = 10;
		if(niuPoint2 > 10) niuPoint2 = 10;
		if(niuPoint3 > 10) niuPoint3 = 10;
		if(niuPoint4 > 10) niuPoint4 = 10;
		if(niuPoint5 > 10) niuPoint5 = 10;
		
		int remainder = 0;
		int temp, temp2;
		if((niuPoint1 + niuPoint2 + niuPoint3) % 10 == 0){ //KJ1054
			remainder = (niuPoint4 + niuPoint5) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint1 + niuPoint2 + niuPoint4) % 10 == 0){//K7632
			temp = cardsData[3];
			cardsData[3] = cardsData[4];
			cardsData[4] = temp;
			
			remainder = (niuPoint3 + niuPoint5) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint1 + niuPoint2 + niuPoint5) % 10 == 0){//K954A
			temp = cardsData[3];
			temp2 = cardsData[4];
			cardsData[3] = cardsData[5];
			cardsData[4] = temp;
			cardsData[5] = temp2;
			
			remainder = (niuPoint3 + niuPoint4) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint1 + niuPoint3 + niuPoint4) % 10 == 0){//K9643
			temp = cardsData[2];
			cardsData[2] = cardsData[3];
			cardsData[3] = cardsData[4];
			cardsData[4] = temp;
			
			remainder = (niuPoint2 + niuPoint5) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint1 + niuPoint3 + niuPoint5) % 10 == 0){//K9753
			temp = cardsData[2];
			temp2 = cardsData[4];
			cardsData[2] = cardsData[3];
			cardsData[3] = cardsData[5];
			cardsData[4] = temp;
			cardsData[5] = temp2;
			
			remainder = (niuPoint2 + niuPoint4) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint1 + niuPoint4 + niuPoint5) % 10 == 0){//K9864
			temp = cardsData[2];
			temp2 = cardsData[3];
			cardsData[2] = cardsData[4];
			cardsData[3] = cardsData[5];
			cardsData[4] = temp;
			cardsData[5] = temp2;
			
			remainder = (niuPoint2 + niuPoint3) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint2 + niuPoint3 + niuPoint4) % 10 == 0){//97764
			temp = cardsData[1];
			cardsData[1] = cardsData[2];
			cardsData[2] = cardsData[3];
			cardsData[3] = cardsData[4];
			cardsData[4] = temp;
			
			remainder = (niuPoint1 + niuPoint5) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint2 + niuPoint3 + niuPoint5) % 10 == 0){//9722A
			temp = cardsData[1];
			temp2 = cardsData[4];
			cardsData[1] = cardsData[2];
			cardsData[2] = cardsData[3];
			cardsData[3] = cardsData[5];
			cardsData[4] = temp;
			cardsData[5] = temp2;
			
			remainder = (niuPoint1 + niuPoint4) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint2 + niuPoint4 + niuPoint5) % 10 == 0){//105432
			temp = cardsData[1];
			temp2 = cardsData[3];
			cardsData[1] = cardsData[2];
			cardsData[2] = cardsData[4];
			cardsData[3] = cardsData[5];
			cardsData[4] = temp;
			cardsData[5] = temp2;
			
			remainder = (niuPoint1 + niuPoint3) % 10;
			setNiu(cardsData, remainder);
		} else if((niuPoint3 + niuPoint4 + niuPoint5) % 10 == 0){//97532
			temp = cardsData[1];
			temp2 = cardsData[2];
			cardsData[1] = cardsData[3];
			cardsData[2] = cardsData[4];
			cardsData[3] = cardsData[5];
			cardsData[4] = temp;
			cardsData[5] = temp2;
			
			remainder = (niuPoint1 + niuPoint2) % 10;
			setNiu(cardsData, remainder);
		} else {//无牛
			cardsData[0] = Const.GameCardType.NORMAL.value();
		}
		
		
		return cardsData;
	}
	
	/**
	 * 设置牌型中是牛几
	 * @param cardsData 牌数据
	 * @param remainder 除开3张牌后的牛余数
	 */
	private static void setNiu(int[] cardsData, int remainder){
		if(0 == remainder)//牛牛
			cardsData[0] = Const.GameCardType.DN_NN.value();
		else 
			cardsData[0] = Const.GameCardType.DN_N1.value() + remainder - 1;//牛1〜牛9
	}
	
	/**
	 * 金花牌型
	 * @param cardsData 初步整理后的牌数据
	 * @return 金花牌型
	 */
	private static int[] checkJINHUAType(int[] cardsData){
		if(cardsData.length != 4)
			return null;
		
		Const.CardBaseType baseType = Const.CardBaseType.nameOfValue(cardsData[0]);
		cardsData[0] = Const.GameCardType.NORMAL.value();
		
		int point1 = getPointByCardID(cardsData[1]);
		int point2 = getPointByCardID(cardsData[2]);
		int point3 = getPointByCardID(cardsData[3]);
		Const.Suit suit1 = getSuitByCardID(cardsData[1]);
		Const.Suit suit2 = getSuitByCardID(cardsData[2]);
		Const.Suit suit3 = getSuitByCardID(cardsData[3]);
		
		if(Const.CardBaseType.TRANGLE_SINGLE == baseType){
			cardsData[0] = Const.GameCardType.JH_TRANGLE.value();
		} else if(Const.CardBaseType.SINGLE == baseType){
			//同花
			boolean isSameSuits = suit1 == suit2 && suit2 == suit3;
			//顺子
			boolean isQueue = point1 == 1 + point2
							&& point2 == 1 + point3;
			
			//特殊顺子Ａ当1用  Ａ32
			if(!isQueue && point1 == Const.CARD_POINT_A){
				isQueue = point2 == 3 && point3 == 2;
				if(isQueue){
					int temp = cardsData[1];
					cardsData[1] = cardsData[2];
					cardsData[2] = cardsData[3];
					cardsData[3] = temp;//A
				}
			}
			
			if(isSameSuits && isQueue){//同花顺
				cardsData[0] = Const.GameCardType.JH_SAME_SUITS_QUEUE.value();
			} else if(isSameSuits){//同花
				cardsData[0] = Const.GameCardType.JH_SAME_SUITS.value();
			} else if(isQueue){//顺子
				cardsData[0] = Const.GameCardType.JH_QUEUE.value();
			}
		} else if(Const.CardBaseType.DOUBLE == baseType){
			cardsData[0] = Const.GameCardType.JH_DOUBLE.value();
		}
		
		return cardsData;
	}
	
	/**
	 * 根据牌id获取牌的点数
	 * @param id 牌id
	 * @param AisOne A当1用
	 * @return 牌点数
	 */
	public static int getPointByCardID(int id){
		
		if(Const.CARD_ID_JOKER1 == id)
			return Const.CARD_POINT_JOKER1;
		if (Const.CARD_ID_JOKER2 == id)
			return Const.CARD_POINT_JOKER2;
		if(id >= Const.CARD_ID_MIN && id <= Const.CARD_ID_MAX){
			return id / 4 + 2;
		}
			
		return Const.CARD_POINT_2;//最小的牌
	}
	
	/**
	 * 根据牌id获取牌的花色
	 * @param id 牌id
	 * @return 牌花色
	 */
	public static Const.Suit getSuitByCardID(int id){
		if(Const.CARD_ID_JOKER1 == id)
			return Const.Suit.JOKER1;
		if(Const.CARD_ID_JOKER2 == id)
			return Const.Suit.JOKER2;
		
		return Const.Suit.nameOfValue(id % 4);
	}
	
	/**
	 * 获取牌 含花色和点数
	 * @param id 牌ID
	 * @return 
	 */
	public static String getCardNameByID(int id){
		Const.Suit s = getSuitByCardID(id);
		if(s == Const.Suit.JOKER1 || s == Const.Suit.JOKER2)
			return s.getName();
		
		return s.getName()+Const.CARD_NAME[getPointByCardID(id) - 2];
	}
	
	/**
	 * 打印出牌列表
	 * @param cards 牌ID列表
	 */
	public static void printCards(int[] cards){
		int count = cards == null ? 0 : cards.length;
		if(count == 0) return;
		
		int i = 0;
		for(int cardId : cards){
			System.out.print(getCardNameByID(cardId) + "\t");
			if(++i % 10 == 0)System.out.println();
		}
		System.out.println();
	}
}
