package com.gz.gamecity.gameserver.config;

import java.util.Random;

import com.alibaba.fastjson.JSONArray;
import com.gz.util.JsonFileRead;

public class AllTemplate {
	//private static String PATH="design/jsondb/";
	private static String PATH="./conf/gameData/";
	protected static Random r = new Random(System.currentTimeMillis());
	
	public static void initTemplates() {
		AllTemplate.loadFile();
	}

	//加载XXX配置表
	//----------------------系统配置表-------------------------------
	private static JSONArray system_login_announce_jsonArray = new JSONArray();//登录公告
	private static JSONArray hall_vipLever_infoArray = new JSONArray();//vip等级信息
	
	//----------------------游戏配置表-------------------------------
	private static JSONArray fruit_ordinary_jsonArray = new JSONArray();//水果机
	private static JSONArray fruit_special_jsonArray = new JSONArray();//水果机
	private static JSONArray luckyWheel_level_jsonArray = new JSONArray();//幸运大转盘
	private static JSONArray luckyWheel_probobalitie1_jsonArray = new JSONArray();//幸运大转盘
	private static JSONArray luckyWheel_probobalitie2_jsonArray = new JSONArray();//幸运大转盘
	private static JSONArray luckyWheel_probobalitie3_jsonArray = new JSONArray();//幸运大转盘
	private static JSONArray Mahjong_probability1_jsonArray = new JSONArray();//麻将
	private static JSONArray Mahjong_probability2_jsonArray = new JSONArray();//麻将
	private static JSONArray Mahjong_level_jsonArray = new JSONArray();//麻将
	private static JSONArray spadeA_probobality_jsonArray = new JSONArray();//黑桃A
	private static JSONArray laba_level_jsonArray = new JSONArray();//拉霸
	private static JSONArray laba_probobality1_jsonArray = new JSONArray();//拉霸
	private static JSONArray laba_probobality2_jsonArray = new JSONArray();//拉霸
	private static JSONArray niuniu_level_jsonArray = new JSONArray();

	private static JSONArray niuniu_probobality1 = new JSONArray();
	private static JSONArray niuniu_probobality2 = new JSONArray();
	private static JSONArray vipLevel_jsonArray=new JSONArray();
	private static JSONArray fruit_level = new JSONArray();
	private static JSONArray fruit_odds = new JSONArray();
	private static JSONArray shop_config = new JSONArray();
	private static JSONArray gift_config = new JSONArray();
	private static JSONArray sign_config = new JSONArray();
	private static JSONArray exp_config = new JSONArray();
	private static JSONArray heads_config = new JSONArray();

	
	
	private static JSONArray texas_level_jsonArray = new JSONArray();



	private static void loadFile() {
		fruit_ordinary_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"fruit_probabality_ordinary_junior.json");
		fruit_special_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"fruit_probabality_special_junior.json");
		luckyWheel_level_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"luckyWheel_level.json");
		luckyWheel_probobalitie1_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"luckyWheel_probobalitie1.json");
		luckyWheel_probobalitie2_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"luckyWheel_probobalitie2.json");
		luckyWheel_probobalitie3_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"luckyWheel_probobalitie3.json");
		Mahjong_probability1_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"Mahjong_probability1.json");
		Mahjong_probability2_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"Mahjong_probability2.json");
		Mahjong_level_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"Mahjong_level.json");
		spadeA_probobality_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"spadeA_probobality.json");
		laba_level_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"LaBa_level.json");
		laba_probobality1_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"LaBa_probobality1.json");
		laba_probobality2_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"LaBa_probobality2.json");

		system_login_announce_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"system_login_announce.json");
		hall_vipLever_infoArray = JsonFileRead.getInstance().readJsonArray(PATH+"hall_vipLever_Info.json");
		niuniu_level_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH+"niuniu_level.json");
		niuniu_probobality1 = JsonFileRead.getInstance().readJsonArray(PATH+"niuniu_probobality1.json");
		niuniu_probobality2 = JsonFileRead.getInstance().readJsonArray(PATH+"niuniu_probobality2.json");
		vipLevel_jsonArray=JsonFileRead.getInstance().readJsonArray(PATH+"vipLevel.json");
		fruit_level = JsonFileRead.getInstance().readJsonArray(PATH+"fruit_level.json");
		fruit_odds = JsonFileRead.getInstance().readJsonArray(PATH+"fruit_odds.json");
		

		texas_level_jsonArray = JsonFileRead.getInstance().readJsonArray(PATH + "texas_level.json");
		shop_config = JsonFileRead.getInstance().readJsonArray(PATH + "shop_config.json");
		gift_config = JsonFileRead.getInstance().readJsonArray(PATH + "gift_config.json");
		sign_config = JsonFileRead.getInstance().readJsonArray(PATH + "sign.json");
		exp_config = JsonFileRead.getInstance().readJsonArray(PATH + "exp.json");
		heads_config = JsonFileRead.getInstance().readJsonArray(PATH + "heads.json");
		

//		System.out.println("   is : "  + fruit_special_jsonArray);
//		System.out.println("   is : " + system_login_announce_jsonArray);
//		System.out.println("   is : " + luckyWheel_level_jsonArray);
//		System.out.println("   is : " + luckyWheel_probobalitie1_jsonArray);
//		System.out.println("   is : " + luckyWheel_probobalitie2_jsonArray);
//		System.out.println("   is : " + luckyWheel_probobalitie3_jsonArray);
//		System.out.println("   is : " + majiang_probobality_jsonArray);
//		System.out.println("   is : " + spadeA_probobality_jsonArray);
//		System.out.println("   is : " + laba_probobality1_jsonArray);
//		System.out.println("   is : " + laba_probobality2_jsonArray);
	}
	
	
	public static JSONArray getHeads_config() {
		return heads_config;
	}


	public static JSONArray getExp_config() {
		return exp_config;
	}


	public static JSONArray getSign_config() {
		return sign_config;
	}


	public static JSONArray getFruit_odds() {
		return fruit_odds;
	}


	public static Object GetTRole(int age, int occ) {
		return null;
	}
	
	
	
	
	public static JSONArray getGift_config() {
		return gift_config;
	}


	public static JSONArray getHall_vipLever_infoArray() {
		return hall_vipLever_infoArray;
	}


	public static JSONArray getLaba_level_jsonArray(){
		return laba_level_jsonArray;
	}
	
	public static JSONArray getLaba_probobality1_jsonArray() {
		return laba_probobality1_jsonArray;
	}

	public static JSONArray getLaba_probobality2_jsonArray() {
		return laba_probobality2_jsonArray;
	}

	public static JSONArray getSpadeA_probobality_jsonArray() {
		return spadeA_probobality_jsonArray;
	}

	public static JSONArray getSystem_login_announce_jsonArray() {
		return system_login_announce_jsonArray;
	}
	
	public static JSONArray getLuckyWheel_level_jsonArray() {
		return luckyWheel_level_jsonArray;
	}
	
	public static JSONArray getLuckyWheel_probobalitie1_jsonArray() {
		return luckyWheel_probobalitie1_jsonArray;
	}
	
	public static JSONArray getLuckyWheel_probobalitie2_jsonArray() {
		return luckyWheel_probobalitie2_jsonArray;
	}

	public static JSONArray getLuckyWheel_probobalitie3_jsonArray() {
		return luckyWheel_probobalitie3_jsonArray;
	}

	public static JSONArray getMahjong_probability1_jsonArray() {
		return Mahjong_probability1_jsonArray;
	}
	
	public static JSONArray getMahjong_probability2_jsonArray() {
		return Mahjong_probability2_jsonArray;
	}

	public static JSONArray getMahjong_level_jsonArray() {
		return Mahjong_level_jsonArray;
	}
	
	public static JSONArray getFruitOrdinaryJsonArray() {
		return fruit_ordinary_jsonArray;
	}
	
	public static JSONArray getFruitSpecialJsonArray() {
		return fruit_special_jsonArray;
	}
	
	public static JSONArray getNiuniu_level_jsonArray() {
		return niuniu_level_jsonArray;
	}
	
	public static JSONArray getTexas_level_jsonArray() {
		return texas_level_jsonArray;
	}
	

	public static JSONArray getNiuniu_probobality1() {
		return niuniu_probobality1;
	}


	public static JSONArray getNiuniu_probobality2() {
		return niuniu_probobality2;
	}

	public static JSONArray getvipLevel_jsonArray(){
		return vipLevel_jsonArray;
	}
	
	public static JSONArray getFruit_level() {
		return fruit_level;
	}


	public static JSONArray getShop_config() {
		return shop_config;
	}


	
	

//	public static void main(String[] args) {
//		load_test_t();
//	}
}
