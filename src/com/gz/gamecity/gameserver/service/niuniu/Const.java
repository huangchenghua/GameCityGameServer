package com.gz.gamecity.gameserver.service.niuniu;

public final class Const {
	
	//牌名
	public final static String[] CARD_NAME = {
		"2","3","4","5","6","7","8","9","10","J","Q","K","A", "JOKER","JOKER2"	
	};
	
	/**
	 * 花色
	 * @author boogle
	 *
	 */
	public static enum Suit {//♠︎♣︎♥︎♦︎♤♧♡♢
		DIAMOND	(0, "♦︎"),	//方片
		CLUB	(1, "♣︎"),	//梅花
		HEART	(2, "♥︎"),	//红心
		SPADE	(3, "♠︎"),	//黑桃
		
		JOKER1	(4, "joker"),	//小鬼
		JOKER2	(5, "JOKER");	//大鬼
		
		private int value;
		private String name;
		private Suit(int v, String n){
			this.value = v;
			this.name = n;
		}
		
		public int value(){
			return this.value;
		}
		
		public String getName(){
			return this.name;
		}
		
		/**
		 * 根据值获取花色名
		 * @param suitValue 花色值
		 * @return 花色名
		 */
		public static Suit nameOfValue(int suitValue){
			if(DIAMOND.value == suitValue)
				return DIAMOND; 
			else if(CLUB.value == suitValue)
				return CLUB;
			else if(HEART.value == suitValue)
				return HEART;
			else if(SPADE.value == suitValue)
				return SPADE;
			else if(JOKER1.value == suitValue)
				return JOKER1;
			else if(JOKER2.value == suitValue)
				return JOKER2;
			
			return DIAMOND;
		}
	}
	
	//版面点数
	public final static int CARD_POINT_A1 = 1;	//A当1用 
	public final static int CARD_POINT_2 = 2;	//2 
	public final static int CARD_POINT_K = 13; 	//K
	public final static int CARD_POINT_A = 14; 	//Ａ 特殊情况Ａ当1点算
	public final static int CARD_POINT_JOKER1 = 15;//小王
	public final static int CARD_POINT_JOKER2 = 16;//大王
	
	/*牌索引值(54张牌) 含花色和点数 除以4商再加2决定牌值，余数决定花色（大小王需单独判定）
	 如43代表黑桃Ｑ（43/4=10余3，  10+2为Ｑ ， 余3为黑桃）
	 
	 	|	2	3	4	5	6	7	8	9	10	Ｊ	Ｑ	Ｋ	Ａ	小鬼	大鬼
	 ---|－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
	  方	|	0	4	8	12	16	20	24	28	32	36	40	44	48	52	53
	  梅	|	1	5	9	13	17	21	25	29	33	37	41	45	49
	  红	|	2	6	10	14	18	22	26	30	34	38	42	46	50
	  黑	|	3	7	11	15	19	23	27	31	35	39	43	47	51
	 
	*/
	public final static int CARD_ID_MIN = 0;//方片2
	public final static int CARD_ID_MAX = 51;//黑桃A
	public final static int CARD_ID_JOKER1 = 52;//小王
	public final static int CARD_ID_JOKER2 = 53;//大王
	
	/**
	 * 基本牌型 一手牌可能同时含有几种，但有四张在，这四张就不再算三张或对子
	 * 5555KK 为（BOMB|DOUBLE）四带对 
	 * @author boogle
	 *
	 */
	public static enum CardBaseType {
		SINGLE			(0), //单牌
		DOUBLE			(1), //对子
		DOUBLE_DOUBLE	(2), //两对
		TRANGLE_SINGLE	(3), //三张
		TRANGLE_DOUBLE	(4), //葫芦
		BOMB			(5); //四条
		
		private int value;
		private CardBaseType(int v){
			this.value = v;
		}
		
		public int value(){
			return this.value;
		}
		
		public static CardBaseType nameOfValue(int value){
			if(SINGLE.value == value)
				return SINGLE;
			else if(DOUBLE.value == value)
				return DOUBLE;
			else if(DOUBLE_DOUBLE.value == value)
				return DOUBLE_DOUBLE;
			else if(TRANGLE_SINGLE.value == value)
				return TRANGLE_SINGLE;
			else if(TRANGLE_DOUBLE.value == value)
				return TRANGLE_DOUBLE;
			else if(BOMB.value == value)
				return BOMB;
			
			return SINGLE;
		}
	}
	
	/**
	 * 游戏玩法
	 * @author boogle
	 *
	 */
	public static enum GameType{
		DEZHOU, //德州
		DOUNIU,	//斗牛
		JINHUA;  //金花
	}
	
	/**
	 * 具体到游戏中的牌型
	 * @author boogle
	 *
	 */
	public static enum GameCardType {
		//=============所有游戏通用=============
		NORMAL						(0,1),//无特殊牌型
		
		//================德州====================
		DZ_DOUBLE					(1,1),//一对
		DZ_DOUBLE_DOUBLE			(2,1),//两对
		DZ_TRANGLE_SINGLE			(3,1),//三条
		DZ_QUEUE					(4,1),//顺子
		DZ_SAME_SUITS				(5,1),//同花
		DZ_TRANGLE_DOUBLE			(6,1),//葫芦
		DZ_BOMB						(7,1),//四条
		DZ_SAME_SUITS_QUEUE 		(8,1),//同花顺
		DZ_ROYAL_SAME_SUITS_QUEUE	(9,1),//皇家同花顺
		
		//================斗牛====================
		DN_N1						(20,1),//牛1
		DN_N2						(21,1),
		DN_N3						(22,1),
		DN_N4						(23,1),
		DN_N5						(24,1),
		DN_N6						(25,1),
		DN_N7						(26,2),
		DN_N8						(27,2),
		DN_N9						(28,2),//牛9
		DN_NN						(29,3),//牛牛
		DN_TRANGLE_SINGLE			(30,3),//三条
		DN_QUEUE					(31,3),//顺子
		DN_SAME_SUITS				(32,3),//同花
		DN_TRANGLE_DOUBLE			(33,4),//葫芦
		DN_BOMB					(34,4),//四条
		DN_SAME_SUITS_QUEUE 		(35,5),//同花顺
		DN_ALL_ROYAL				(36,5),//五花牛
		DN_NN_LESS_10				(37,5),//五小牛
		
		//================金花====================
		JH_DOUBLE					(40,1),//对子
		JH_QUEUE					(41,1),//顺子
		JH_SAME_SUITS				(42,1),//金花
		JH_SAME_SUITS_QUEUE			(43,1),//同花顺
		JH_TRANGLE					(44,1) //三条
		;
		
		private int value;
		private GameCardType(int v,int odds){
			this.value = v;
			this.odds = odds;
		}
		private int odds;
		
		public int getOdds() {
			return odds;
		}

		public int value(){
			return this.value;
		}
		
		public static GameCardType nameOfValue(GameType game, int value){
			switch(game){
			case DEZHOU:
				if(DZ_DOUBLE.value == value)
					return DZ_DOUBLE;
				else if(DZ_DOUBLE_DOUBLE.value == value)
					return DZ_DOUBLE_DOUBLE;
				else if(DZ_TRANGLE_SINGLE.value == value)
					return DZ_TRANGLE_SINGLE;
				else if(DZ_QUEUE.value == value)
					return DZ_QUEUE;
				else if(DZ_SAME_SUITS.value == value)
					return DZ_SAME_SUITS;
				else if(DZ_TRANGLE_DOUBLE.value == value)
					return DZ_TRANGLE_DOUBLE;
				else if(DZ_BOMB.value == value)
					return DZ_BOMB;
				else if(DZ_SAME_SUITS_QUEUE.value == value)
					return DZ_SAME_SUITS_QUEUE;	
				else if(DZ_ROYAL_SAME_SUITS_QUEUE.value == value)
					return DZ_ROYAL_SAME_SUITS_QUEUE;		
				break;
			
			case DOUNIU:
				if(DN_N1.value == value)
					return DN_N1;
				else if(DN_N2.value == value)
					return DN_N2;
				else if(DN_N3.value == value)
					return DN_N3;
				else if(DN_N4.value == value)
					return DN_N4;
				else if(DN_N5.value == value)
					return DN_N5;
				else if(DN_N6.value == value)
					return DN_N6;
				else if(DN_N7.value == value)
					return DN_N7;
				else if(DN_N8.value == value)
					return DN_N8;
				else if(DN_N9.value == value)
					return DN_N9;
				else if(DN_NN.value == value)
					return DN_NN;
				else if(DN_TRANGLE_SINGLE.value == value)
					return DN_TRANGLE_SINGLE;
				else if(DN_QUEUE.value == value)
					return DN_QUEUE;
				else if(DN_SAME_SUITS.value == value)
					return DN_SAME_SUITS;
				else if(DN_TRANGLE_DOUBLE.value == value)
					return DN_TRANGLE_DOUBLE;
				else if(DN_BOMB.value == value)
					return DN_BOMB;
				else if(DN_SAME_SUITS_QUEUE.value == value)
					return DN_SAME_SUITS_QUEUE;
				else if(DN_ALL_ROYAL.value == value)
					return DN_ALL_ROYAL;
				else if(DN_NN_LESS_10.value == value)
					return DN_NN_LESS_10;
				break;
			
			case JINHUA:
				if(JH_DOUBLE.value == value)
					return JH_DOUBLE;
				else if(JH_QUEUE.value == value)
					return JH_QUEUE;
				else if(JH_SAME_SUITS.value == value)
					return JH_SAME_SUITS;
				else if(JH_SAME_SUITS_QUEUE.value == value)
					return JH_SAME_SUITS_QUEUE;
				else if(JH_TRANGLE.value == value)
					return JH_TRANGLE;
				break;
				
			}
			
			return NORMAL;
		}
	}
	
}
