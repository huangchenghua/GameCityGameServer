package com.gz.gamecity.gameserver.db;

import com.gz.gamecity.gameserver.config.ConfigField;
import com.gz.util.Config;

import redis.clients.jedis.Jedis;

public abstract class JBaseDao {
	
	protected static Jedis getConn() {
		try {
			Jedis jedis = JedisConnectionPool.getJedisConn();
			jedis.select(Config.instance().getIValue(ConfigField.DB_INDEX));
			return jedis;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected static void closeConn(Jedis jedis) {
		try {
			if(jedis!=null)
				jedis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
