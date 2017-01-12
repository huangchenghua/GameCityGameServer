package com.gz.gamecity.gameserver.service.texas;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import java.util.TreeMap;

import com.gz.gamecity.gameserver.service.niuniu.PokerCommon;
import com.gz.gamecity.gameserver.table.TexasTable;
import com.gz.gamecity.gameserver.service.niuniu.Const;


public final class TexasUtil {
	
	private static final Logger log = Logger.getLogger(TexasUtil.class);
		
	
	/* same as Const.java
	 * 
	 * 牌索引值(54张牌) 含花色和点数 除以4商再加2决定牌值，余数决定花色（大小王需单独判定）
	 如43代表黑桃Ｑ（43/4=10余3，  10+2为Ｑ ， 余3为黑桃）
	 
	 	|	2	3	4	5	6	7	8	9	10	Ｊ	Ｑ	Ｋ	Ａ	小鬼	大鬼
	 ---|－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
	  方	|	0	4	8	12	16	20	24	28	32	36	40	44	48	52	53
	  梅	|	1	5	9	13	17	21	25	29	33	37	41	45	49
	  红	|	2	6	10	14	18	22	26	30	34	38	42	46	50
	  黑	|	3	7	11	15	19	23	27	31	35	39	43	47	51
	  
	  */
	
	public static enum CardResult {
		HIGH_CARD	(0),	//高牌
		ONE_PAIR	(1),	//一对
		TWO_PAIR	(2),
		THREE_OF_A_KIND	(3),
		STRAIGHT	(4),
		FLUSH		(5),
		FULL_HOUSE		(6),
		FOUR_OF_A_KIND	(7),
		STRAIGHT_FLUSH	(8),
		ROYAL_FLUSH		(9);
		
		
		private int nValue;
		
		private CardResult(int v) {
			this.nValue = v;
		}
		
		public int value() { 
			return this.nValue;
		}
		
		public static CardResult nameOfValue(int v) {
			for (CardResult e : CardResult.values()) {
				if (e.value() == v) {
					return e;
				}
			}
			return HIGH_CARD;
		}
	}
	
	public static int N_HIGHT_TYPE_RADIX = 1000000;
	public static int N_HIGHT_SCORE_RADIX = 10000;
	public static int N_LOW_SCORE_1_RADIX = 100;
	public static int N_LOW_SCORE_2_RADIX = 1;
	
	public static int getCardResultByScore(int nScore) {
		return nScore / N_HIGHT_TYPE_RADIX;
	}
	
	public static int getTotalScore(int nHightType, int nHightScore, int nLowScore1, int nLowScore2) {
		return nHightType * N_HIGHT_TYPE_RADIX + nHightScore * N_HIGHT_SCORE_RADIX + nLowScore1 * N_LOW_SCORE_1_RADIX + N_LOW_SCORE_2_RADIX;
	}
	
	public static boolean isFlush(int[] szCard) {
		int nSuit = -1;
		for (int i = 0; i < szCard.length; ++i) {
			if (nSuit == -1) {
				nSuit = getSuitOfCard(szCard[i]);
			} else if (nSuit != getSuitOfCard(szCard[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	public static int getSuitOfCard(int nCard) {
		return nCard % 4;
	}
	
	public static int getPointOfCard(int nCard) {
		return nCard / 4 + 2;
	}
	public static int CARD_POINT_A = getPointOfCard(48);	// card A point
	
	public static int calcCardScore(int[] szCard, int nHandNum) {
		if (szCard.length != 5) {
			log.error("card's length is invalid[len=" + szCard.length + "]");
			return 0;
		}
		
		//System.arraycopy(szHandCard, 0, szCard, 0, szHandCard.length);
		//System.arraycopy(szPublicCard, 0, szCard, szHandCard.length, szPublicCard.length);

		int nHandPoint1 = getPointOfCard(szCard[0]);
		int nHandPoint2 = getPointOfCard(szCard[1]);
		if (nHandPoint1 < nHandPoint2) {
			int nTmp = nHandPoint1;
			nHandPoint1 = nHandPoint2;
			nHandPoint2 = nTmp;
		}
		
		int nHightType = 0;
		int nHightScore = 0;
		int nLowScore1 = 0;
		int nLowScore2 = 0;
		
		boolean bIsFlush = isFlush(szCard);
		
		boolean bIsStraight = true;
		
		ArrayList<Integer> arraySort = new ArrayList<Integer>();
		for (int i = 0; i < szCard.length; ++i) {
			arraySort.add(getPointOfCard(szCard[i]));
		}
		Collections.sort(arraySort);
		for (int i = 0; i < arraySort.size() - 1; ++i) {
			if ( (arraySort.get(i) + 1) != arraySort.get(i + 1))
			{
				bIsStraight = false;
				break;
			}
		}
		/*
		log.debug("calc card[card=" + nHandPoint1 + ", " + nHandPoint2 + ", " + getPointOfCard(szCard[2]) + ", " + 
						getPointOfCard(szCard[3]) + ", " + getPointOfCard(szCard[4]) + " is_flush=" + bIsFlush + " is_straight=" + bIsStraight + "]");
		*/
		if (bIsFlush && bIsStraight) {
			if (arraySort.get(arraySort.size() - 1) == CARD_POINT_A) {				
				nHightType = CardResult.ROYAL_FLUSH.value();
			} else {
				nHightType = CardResult.STRAIGHT_FLUSH.value();
			}
			nHightScore = arraySort.get(arraySort.size() - 1);
			return getTotalScore(nHightType, nHightScore, 0, 0);
		}
		
		/*
		if (  arraySort.get(0) == arraySort.get(1) && arraySort.get(1) == arraySort.get(2) && arraySort.get(2) == arraySort.get(3)) {
			// four of a kind
			nHightType = CardResult.FOUR_OF_A_KIND.value();
			nHightScore = arraySort.get(0);
			nLowScore1 = arraySort.get(4);
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);			
		} else if (arraySort.get(1) == arraySort.get(2) && arraySort.get(2) == arraySort.get(3) && arraySort.get(3) == arraySort.get(4)) {
			// four of a kind
			nHightType = CardResult.FOUR_OF_A_KIND.value();
			nHightScore = arraySort.get(1);
			nLowScore1 = arraySort.get(0);
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);	
		}
		
		if ( arraySort.get(0) == arraySort.get(1) && arraySort.get(2) == arraySort.get(3) && arraySort.get(3) == arraySort.get(4) ) {
			// full house
			nHightType = CardResult.FULL_HOUSE.value();
			nHightScore = arraySort.get(2);
			nLowScore1	= arraySort.get(0);
			
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		} else if (arraySort.get(0) == arraySort.get(1) && arraySort.get(1) == arraySort.get(2) && arraySort.get(3) == arraySort.get(4)) {
			nHightType = CardResult.FULL_HOUSE.value();
			nHightScore = arraySort.get(0);
			nLowScore1 = arraySort.get(1);
			
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		}*/
		

		
		/*
		// three of a kind
		int nThreeKindCard = -1;
		if ( arraySort.get(2) == arraySort.get(3) && arraySort.get(3) == arraySort.get(4) ) {
			nThreeKindCard = arraySort.get(2);
		} else if (arraySort.get(0) == arraySort.get(1) && arraySort.get(1) == arraySort.get(2) ) {
			nThreeKindCard = arraySort.get(0);
		} else if (arraySort.get(1) == arraySort.get(2) && arraySort.get(2) == arraySort.get(3)) {
			nThreeKindCard = arraySort.get(1);
		}
		
		if (nThreeKindCard != -1) {
			nHightType = CardResult.THREE_OF_A_KIND.value();
			nHightScore = nThreeKindCard;
									
			return getTotalScore(nHightScore, nHightScore, nHandPoint1, nHandPoint2);
		}
		*/
		// 
		int nFourPoint = -1;
		ArrayList<Integer> arrayThree = new ArrayList<Integer>();
		ArrayList<Integer> arrayPair = new ArrayList<Integer>();
		ArrayList<Integer> arraySingle = new ArrayList<Integer>();
		
		int nLastPoint = -1;
		int nLastCnt = -1;
		for ( int i = 0; i < arraySort.size(); ++i) {
			if (nLastPoint == -1){
				nLastPoint = arraySort.get(i);
				nLastCnt = 1;
			} else if (nLastPoint == arraySort.get(i)) {
				nLastCnt += 1;
			} else {
				if (nLastCnt == 4) {
					nFourPoint = nLastPoint;
				} else if (nLastCnt == 3) {
					arrayThree.add(nLastPoint);
				} else if (nLastCnt == 2) {
					arrayPair.add(nLastPoint);
				} else if (nLastCnt == 1) {
					arraySingle.add(nLastPoint);
				}
				nLastPoint = arraySort.get(i);
				nLastCnt = 1;
			}
		}
		// nLastPoint input array
		if (nLastCnt == 4) {
			nFourPoint = nLastPoint;
		} else if (nLastCnt == 3) {
			arrayThree.add(nLastPoint);
		} else if (nLastCnt == 2) {
			arrayPair.add(nLastPoint);
		} else if (nLastCnt == 1) {
			arraySingle.add(nLastPoint);
		}
		
		
		// four kind
		if (nFourPoint != -1) {
			nHightType = CardResult.FOUR_OF_A_KIND.value();
			nHightScore = nFourPoint;
			if (nHandPoint1 != nFourPoint){
				nLowScore1 = nHandPoint1;
			} else if (nHandPoint2 != nFourPoint) {
				nLowScore1 = nHandPoint2;
			}
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		}
		
		
		// full house
		if (arrayThree.size() == 1 && arrayPair.size() == 1) {
			nHightType = CardResult.FULL_HOUSE.value();
			nHightScore = arrayThree.get(0);
			nLowScore1	= arrayPair.get(0);
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		} 
		
		if (bIsFlush) {
			// flush 
			return getTotalScore(CardResult.FLUSH.value(), 0, nHandPoint1, nHandPoint2);
		}
		
		// straight
		if (bIsStraight) {
			return getTotalScore(CardResult.STRAIGHT.value(), arraySort.get(arraySort.size() - 1), 0, 0);
		}
		
		
		// three kind
		if (arrayThree.size() == 1 && arrayPair.size() == 0) {
			nHightType = CardResult.THREE_OF_A_KIND.value();
			nHightScore = arrayThree.get(0);
			if (nHandPoint1 != arrayThree.get(0)) {
				nLowScore1 = nHandPoint1;
			}
			if (nHandPoint2 != arrayThree.get(0)) {
				nLowScore2 = nHandPoint2;
			}
			if (nLowScore1 < nLowScore2) {
				int nTmp = nLowScore1;
				nLowScore1 = nLowScore2;
				nLowScore2 = nTmp;
			}
						
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		}
		
		//two pair
		if (arrayPair.size() == 2) {
			// two pair

			nHightType = CardResult.TWO_PAIR.value();
			nHightScore = arrayPair.get(1);
			nLowScore1 = arrayPair.get(0);
			
			if (nHandPoint1 == arraySingle.get(0) || nHandPoint2 == arraySingle.get(0) ) {
				nLowScore2 = arraySingle.get(0);
			}
			
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
			
		}
		// one pair
		if (arrayPair.size() == 1) {
			
			nHightType = CardResult.ONE_PAIR.value();
			nHightScore = arrayPair.get(0);
			
			if (nHandPoint1 != arrayPair.get(0)) {
				nLowScore1 = nHandPoint1;
				if (nHandPoint2 != arrayPair.get(0)) {
					nLowScore2 = nHandPoint2;
				}
			} else if (nHandPoint2 != arrayPair.get(0)) {
				nLowScore1 = nHandPoint2;
			}
			
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		} 
		
		// high single card
		if (arrayPair.size() == 0 )
		{
			nHightType = CardResult.HIGH_CARD.value();
			nHightScore = nHandPoint1;
			nLowScore1 = nHandPoint2;
			return getTotalScore(nHightType, nHightScore, nLowScore1, nLowScore2);
		}
		
		log.error("error logic clac texas score[four_point=" + nFourPoint + " three_size=" + arrayThree.size() + " pair_size=" + arrayPair.size() + 
				 " single_size=" + arraySingle.size() + "]");
		return 0;
	}
	
}
