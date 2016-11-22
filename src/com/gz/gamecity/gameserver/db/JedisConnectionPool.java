package com.gz.gamecity.gameserver.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisConnectionPool {
	private static JedisPool pool = null;
	
	public static synchronized void init(String host,int port, String password){
		if (pool == null) {
        	JedisPoolConfig config = new JedisPoolConfig();
	        //最大空闲连接数, 应用自己评估，不要超过AliCloudDB for Redis每个实例最大的连接数
	        config.setMaxIdle(200);
	        //最大连接数, 应用自己评估，不要超过AliCloudDB for Redis每个实例最大的连接数
	        config.setMaxTotal(300);
	        config.setTestOnBorrow(false);
	        config.setTestOnReturn(false);
	        
	        pool = new JedisPool(config, host, port, 3000, password);
        }
	}
	
	public static synchronized Jedis getJedisConn(){
		if(pool!=null)
			return pool.getResource();
		return null;
	}
	
	
}
