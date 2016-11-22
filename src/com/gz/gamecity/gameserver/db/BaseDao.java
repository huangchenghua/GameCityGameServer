package com.gz.gamecity.gameserver.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public abstract class BaseDao {
	
	protected static Jedis getConn() {
		try {
			Jedis jedis = JedisConnectionPool.getJedisConn();
			
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
