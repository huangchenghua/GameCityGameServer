package com.gz.gamecity.gameserver.service.niuniu;

public class Test2 {

	public static void main(String[] args) {
//		int[] cards = PokerCommon.initCards();
		
		
//		PokerCommon.shuffle(cards);
//		
//		int[] poker=new int[5];
//		System.arraycopy(cards, 0, poker, 0, 5);
//		Utils.printArray(poker, 5);
//		PokerCommon.printCards(poker);
//		int[] baseType = PokerCommon.getRankedCardBaseType(Const.GameType.DOUNIU, poker);
//		int[] tmp= new int[baseType.length];
//		System.arraycopy(baseType, 0, tmp, 0, baseType.length);
//		Utils.printArray(baseType, 6);
//		System.arraycopy(baseType, 1, poker, 0, 5);
//		PokerCommon.printCards(poker);
//		
//		int[] cardsData = PokerCommon.checkDOUNIUType(tmp);
//		
//		Utils.printArray(baseType, 6);
		
		
		t5();
	}

	
	private static void t1(){
		int[] cards = PokerCommon.initCardsExceptJoker();
		PokerCommon.shuffle(cards);
		Utils.printArray(cards, cards.length);
		for(int i=0;i<cards.length;i++){
			//48-51是A，因为A当1用，所以要减掉48
			if(cards[i]>=48 && cards[i]<=51){
				cards[i] = cards[i]-48;
			}
			else
				cards[i] = cards[i]+4;
		}

		Utils.printArray(cards, cards.length);
	}
	
	private static void t2(){
		int[] cards = {11,13,49,51,48};
		cards = new int[]{22,32,35,9,1};
		cards = new int[]{30,14	,47,5,4};
		cards = new int[]{10,39	,34,16,40};
		cards = new int[]{29,21,6,36,26};
		PokerCommon.printCards(cards);
	}
	
	private static void t3(){
		int index[] = new int[5];
		index = Utils.getRandomIntArrayDistinct(0, 5, 5);
		PokerCommon.shuffle(index);
		Utils.printArray(index, 5);
	}
	
	private static void t4(){
		int cards[] = {20,7,30,11,19};
		PokerCommon.printCards(cards);
		
		int[] cardsData = PokerCommon.getGameCardsData(Const.GameType.DOUNIU, cards);
		System.out.println(Const.GameCardType.nameOfValue(Const.GameType.DOUNIU, cardsData[0]));
		System.out.println("hasSame="+hasSame(cards));
	}
	
	
	public static int hasSame(int[] arr){
		for(int i=0;i<arr.length;i++){
			for(int j=i+1;j<arr.length;j++){
				if(arr[i] == arr[j])
					return arr[i];
			}
		}
		return -1;
	}
	
	private static void t5(){
		int[] poker_index=new int[5];
		int[] index_player_poker={1,2,3,4};
		//PokerCommon.shuffle(index_player_poker);
		int index=4;
		
		System.arraycopy(index_player_poker, 0, poker_index, 0, index);
		poker_index[index]=0;
		System.arraycopy(index_player_poker, index, poker_index, index+1, 4-index);
		Utils.printArray(poker_index, 5);
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
