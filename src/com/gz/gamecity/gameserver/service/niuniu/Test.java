package com.gz.gamecity.gameserver.service.niuniu;


public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

//		int[] cards = PokerCommon.initCardsExceptJoker();
//		Utils.printArray(cards, 10);
//		PokerCommon.shuffle(cards);
//		Utils.printArray(cards, 10);
		
		
		Const.GameType game = Const.GameType.DOUNIU;
		//for(int i = 50; i >= 0; i--){
		int[] cards = Utils.getRandomIntArrayDistinct(0, 52, 5);
//		int[] cards = {0,1,2,3,4};
		Utils.printArray(cards, 10);
		PokerCommon.printCards(cards);
		int[] cardsData = PokerCommon.getGameCardsData(game, cards);
		int[] rankedCards = new int[cardsData.length - 1];
		System.arraycopy(cardsData, 1, rankedCards, 0, rankedCards.length);
		System.out.println("game:" + game + "\tcardType:" + Const.GameCardType.nameOfValue(game, cardsData[0]));
		PokerCommon.printCards(rankedCards);
		System.out.println("================================");
	}
		
	//}

	
}
