package com.gz.gamecity.gameserver.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.gz.dbpools.ConnectionFactory;

public abstract class BaseDao {
	protected Connection getConn() {
		Connection conn=null;
		try {
			conn = ConnectionFactory.lookup("game").getConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	protected void close(ResultSet rs,PreparedStatement pstmt,Connection conn) {
		try {
			if(rs!=null)
				rs.close();
		} catch (Exception e) {
		}
		try {
			if(pstmt!=null)
				pstmt.close();
		} catch (Exception e) {
		}
		try {
			if(conn!=null)
				conn.close();
		} catch (Exception e) {
		}
	}
}
