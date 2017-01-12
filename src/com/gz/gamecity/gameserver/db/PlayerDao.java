package com.gz.gamecity.gameserver.db;

import java.sql.Connection;
import java.sql.PreparedStatement;


public class PlayerDao extends BaseDao {
	
	public void recordCoinChange(String player_uuid,long coin,long change,int type,String uuid_log){
		Connection conn=null;
		PreparedStatement pstmt = null;
		try {
			String sql="insert into player_coin_log values(?,?,?,?,?,now())";
			conn=getConn();
			pstmt=conn.prepareStatement(sql);
			pstmt.setString(1, player_uuid);
			pstmt.setString(2, uuid_log);
			pstmt.setLong(3, coin);
			pstmt.setLong(4, change);
			pstmt.setInt(5, type);
			pstmt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			close(null,pstmt,conn);
		}
	}
}
