package com.cosog.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.orm.hibernate5.SessionFactoryUtils;

import com.cosog.model.DataSourceConfig;
import com.cosog.model.DataWriteBackConfig;
import com.cosog.task.MemoryDataManagerTask;

import oracle.sql.CLOB;

public class OracleJdbcUtis {

	public static BasicDataSource outerDataSource=null;
	
	public static BasicDataSource outerDataWriteBackDataSource=null;
	
	private static void initOuterDataSource(){
		
		DataSourceConfig dataSourceConfig=MemoryDataManagerTask.getDataSourceConfig();
		
		if(dataSourceConfig!=null && dataSourceConfig.isEnable()){
			
			outerDataSource = new BasicDataSource();
			
			outerDataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");

			outerDataSource.setUrl("jdbc:oracle:thin:@"+dataSourceConfig.getIP()+":"+dataSourceConfig.getPort()+(dataSourceConfig.getVersion()>=12?"/":":")+dataSourceConfig.getInstanceName()+"");

			outerDataSource.setUsername(dataSourceConfig.getUser());

			outerDataSource.setPassword(dataSourceConfig.getPassword());

			outerDataSource.setInitialSize(5); // 初始化连接数

			outerDataSource.setMaxIdle(10); // 最大空闲连接数

			outerDataSource.setMinIdle(5); // 最小空闲连接数

			outerDataSource.setMaxIdle(100); // 最大连接数
		}
	}
	
	private static void initDataWriteBackDataSource(){
		
		DataWriteBackConfig dataWriteBackConfig=MemoryDataManagerTask.getDataWriteBackConfig();
		
		if(dataWriteBackConfig!=null && dataWriteBackConfig.isEnable()){
			
			outerDataWriteBackDataSource = new BasicDataSource();
			
			outerDataWriteBackDataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");

			outerDataWriteBackDataSource.setUrl("jdbc:oracle:thin:@"+dataWriteBackConfig.getIP()+":"+dataWriteBackConfig.getPort()+(dataWriteBackConfig.getVersion()>=12?"/":":")+dataWriteBackConfig.getInstanceName()+"");

			outerDataWriteBackDataSource.setUsername(dataWriteBackConfig.getUser());

			outerDataWriteBackDataSource.setPassword(dataWriteBackConfig.getPassword());

			outerDataWriteBackDataSource.setInitialSize(5); // 初始化连接数

			outerDataWriteBackDataSource.setMaxIdle(10); // 最大空闲连接数

			outerDataWriteBackDataSource.setMinIdle(5); // 最小空闲连接数

			outerDataWriteBackDataSource.setMaxIdle(100); // 最大连接数
		}
	}
	
	public static Connection getOuterConnection() throws SQLException{
		Connection conn=null;
		if(outerDataSource==null){
			initOuterDataSource();
		}
		if(outerDataSource!=null){
			conn=outerDataSource.getConnection();
		}
		return conn;
	}
	
	public static Connection getDataWriteBackConnection() throws SQLException{
		Connection conn=null;
		if(outerDataWriteBackDataSource==null){
			initDataWriteBackDataSource();
		}
		if(outerDataWriteBackDataSource!=null){
			conn=outerDataWriteBackDataSource.getConnection();
		}
		return conn;
	}
	
	
	public static Connection getConnection(){  
        try{
        	String driver=Config.getInstance().configFile.getAp().getDatasource().getDriver();
            String url = Config.getInstance().configFile.getAp().getDatasource().getDriverUrl(); 
            String username = Config.getInstance().configFile.getAp().getDatasource().getUser();
            String password = Config.getInstance().configFile.getAp().getDatasource().getPassword();  
            Class.forName(driver).newInstance();  
            Connection conn = DriverManager.getConnection(url, username, password);  
            return conn;  
        }  
        catch (Exception e){  
            StringManagerUtils.printLog(e.getMessage());  
            return null;  
        }  
    }
	
	public static Connection getConnection(String driver,String url,String username,String password){  
        try{
            Class.forName(driver).newInstance();  
            Connection conn = DriverManager.getConnection(url, username, password);  
            return conn;  
        }  
        catch (Exception e){  
            StringManagerUtils.printLog(e.getMessage());  
            return null;  
        }  
    }
	
	public static void closeDBConnection(Connection conn,PreparedStatement pstmt){  
        if(conn != null){  
            try{           
            	if(pstmt!=null)
            		pstmt.close();  
                conn.close();  
            }catch(SQLException e){  
                StringManagerUtils.printLog("closeDBConnectionError!");  
                e.printStackTrace();  
            }finally{  
                try{
                	if(pstmt!=null)
                		pstmt.close();
                }catch(SQLException e){  
                    e.printStackTrace();  
                }  
                conn = null;  
            }  
        }  
    }
	
	public static void closeDBConnection(Connection conn,PreparedStatement pstmt,ResultSet rs){  
        if(conn != null){  
            try{           
            	if(pstmt!=null)
            		pstmt.close();  
            	if(rs!=null)
            		rs.close();
                conn.close();  
            }catch(SQLException e){  
                StringManagerUtils.printLog("closeDBConnectionError!");  
                e.printStackTrace();  
            }finally{  
                try{
                	if(pstmt!=null)
                		pstmt.close();  
                	if(rs!=null)
                		rs.close();
                }catch(SQLException e){  
                    e.printStackTrace();  
                }  
                conn = null;  
            }  
        }  
    }
	
	public static void closeDBConnection(PreparedStatement pstmt,ResultSet rs){  
		try{           
        	if(pstmt!=null)
        		pstmt.close();  
        	if(rs!=null)
        		rs.close();  
        }catch(SQLException e){  
            StringManagerUtils.printLog("closeDBConnectionError!");  
            e.printStackTrace();  
        }finally{  
            try{
            	if(pstmt!=null)
            		pstmt.close();  
            	if(rs!=null)
            		rs.close();
            }catch(SQLException e){  
                e.printStackTrace();  
            }
        } 
    }
	
	public static void closeDBConnection(Connection conn,Statement stmt,PreparedStatement pstmt,ResultSet rs){  
        if(conn != null){  
            try{
            	if(stmt!=null){
            		stmt.close();
            	}
            	if(pstmt!=null)
            		pstmt.close();  
            	if(rs!=null)
            		rs.close();
                conn.close();  
            }catch(SQLException e){  
                StringManagerUtils.printLog("closeDBConnectionError!");  
                e.printStackTrace();  
            }finally{  
                try{
                	if(stmt!=null){
                		stmt.close();
                	}
                	if(pstmt!=null)
                		pstmt.close();  
                	if(rs!=null)
                		rs.close();
                }catch(SQLException e){  
                    e.printStackTrace();  
                }  
                conn = null;  
            }  
        }  
    }
	
	public static boolean databaseStatus(){
		boolean r=false;
		Connection conn=null;
		try {
			conn=OracleJdbcUtis.getConnection();
			if(conn!=null){
				r=true;
			}
		} catch (Exception e) {
			r=false;
			e.printStackTrace();
		}finally{
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return r;
	}
	
	public static int executeSqlUpdateClob(String sql,List<String> values) throws SQLException {
		int n = 0;
		Connection conn=null;
		PreparedStatement ps = null;
		try {
			conn=OracleJdbcUtis.getConnection();
			ps=conn.prepareStatement(sql);
			for(int i=0;i<values.size();i++){ 
				ps.setCharacterStream(i+1, new java.io.StringReader(values.get(i)), values.get(i).length());
			}
			n=ps.executeUpdate();  
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, ps);
		}
		return n;
	}
	
	public static int executeSqlUpdateClob(String sql,List<String> values,int timeOut) throws SQLException {
		int n = 0;
		Connection conn=null;
		PreparedStatement ps = null;
		try {
			conn=OracleJdbcUtis.getConnection();
			ps=conn.prepareStatement(sql);
			ps.setQueryTimeout(timeOut);
			for(int i=0;i<values.size();i++){ 
				ps.setCharacterStream(i+1, new java.io.StringReader(values.get(i)), values.get(i).length());
			}
			n=ps.executeUpdate();  
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, ps);
		}
		return n;
	}
	
	public static int executeSqlUpdate(String sql){
		int r=0;
		Connection conn=null;
		PreparedStatement pstmt = null;
		try {
			conn=OracleJdbcUtis.getConnection();
			pstmt = conn.prepareStatement(sql);
			r = pstmt.executeUpdate();
		} catch (SQLException e) {
			r=-1;
			e.printStackTrace();
//			StringManagerUtils.printLog(sql);
			System.out.println(StringManagerUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss")+",sql:"+sql);
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, pstmt);
		} 
		return r;
	}
	
	public static int executeSqlUpdate(String sql,int timeOut){
		int r=0;
		Connection conn=null;
		PreparedStatement pstmt = null;
		try {
			conn=OracleJdbcUtis.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setQueryTimeout(timeOut);
			r = pstmt.executeUpdate();
		} catch (SQLException e) {
			r=-1;
			e.printStackTrace();
			StringManagerUtils.printLog(sql);
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, pstmt);
		} 
		return r;
	}
	
	public static List<Object[]> query(String sql) {
		Connection conn=OracleJdbcUtis.getConnection();
		PreparedStatement ps=null;
		ResultSet rs=null;
		ResultSetMetaData rsmd=null;
		List<Object[]> list=new ArrayList<Object[]>();
		
		try {
			ps = conn.prepareStatement(sql);
			rs=ps.executeQuery();
			rsmd=rs.getMetaData();  
			int columnCount=rsmd.getColumnCount();  
			while(rs.next())
			{
				Object[] objs=new Object[columnCount];
				for(int i=0;i<columnCount;i++){
					if(rs.getObject(i+1) instanceof oracle.sql.CLOB || rs.getObject(i+1) instanceof java.sql.Clob ){
						try {
							objs[i]=StringManagerUtils.CLOBtoString2(rs.getClob(i+1));
						} catch (IOException e) {
							objs[i]="";
							e.printStackTrace();
						}
					}else{
						objs[i]=rs.getObject(i+1);
					}
				}
				list.add(objs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(sql);
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, ps, rs);
		}
		return list;
	}
	
	public static List<Object[]> query(String sql,int timeout) {
		Connection conn=OracleJdbcUtis.getConnection();
		PreparedStatement ps=null;
		ResultSet rs=null;
		ResultSetMetaData rsmd=null;
		List<Object[]> list=new ArrayList<Object[]>();
		
		try {
			ps = conn.prepareStatement(sql);
			ps.setQueryTimeout(timeout);
			rs=ps.executeQuery();
			rsmd=rs.getMetaData();  
			int columnCount=rsmd.getColumnCount();  
			while(rs.next())
			{
				Object[] objs=new Object[columnCount];
				for(int i=0;i<columnCount;i++){
					if(rs.getObject(i+1) instanceof oracle.sql.CLOB || rs.getObject(i+1) instanceof java.sql.Clob ){
						try {
							objs[i]=StringManagerUtils.CLOBtoString2(rs.getClob(i+1));
						} catch (IOException e) {
							objs[i]="";
							e.printStackTrace();
						}
					}else{
						objs[i]=rs.getObject(i+1);
					}
				}
				list.add(objs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(sql);
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, ps, rs);
		}
		return list;
	}
	
	public static List<Object[]> query(String sql,String driver,String url,String username,String password) {
		PreparedStatement ps=null;
		ResultSet rs=null;
		ResultSetMetaData rsmd=null;
		List<Object[]> list=new ArrayList<Object[]>();
		Connection conn=OracleJdbcUtis.getConnection(driver, url, username, password);
		try {
			if(conn!=null){
				ps = conn.prepareStatement(sql);
				rs=ps.executeQuery();
				rsmd=rs.getMetaData();  
				int columnCount=rsmd.getColumnCount();  
				while(rs.next())
				{
					Object[] objs=new Object[columnCount];
					for(int i=0;i<columnCount;i++){
						if(rs.getObject(i+1) instanceof oracle.sql.CLOB || rs.getObject(i+1) instanceof java.sql.Clob ){
							try {
								if(rs.getClob(i+1)!=null){
									objs[i]=StringManagerUtils.CLOBtoString2(rs.getClob(i+1));
								}else{
									objs[i]="";
								}
							} catch (IOException e) {
								objs[i]="";
								e.printStackTrace();
							}
						}else{
							objs[i]=rs.getObject(i+1);
						}
					}
					list.add(objs);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(sql);
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, ps, rs);
		}
		return list;
	}
	
	public static List<Object[]> query(String sql,String driver,String url,String username,String password,int timeout) {
		PreparedStatement ps=null;
		ResultSet rs=null;
		ResultSetMetaData rsmd=null;
		List<Object[]> list=new ArrayList<Object[]>();
		Connection conn=OracleJdbcUtis.getConnection(driver, url, username, password);
		try {
			if(conn!=null){
				ps = conn.prepareStatement(sql);
				ps.setQueryTimeout(timeout);
				rs=ps.executeQuery();
				rsmd=rs.getMetaData();  
				int columnCount=rsmd.getColumnCount();  
				while(rs.next())
				{
					Object[] objs=new Object[columnCount];
					for(int i=0;i<columnCount;i++){
						if(rs.getObject(i+1) instanceof oracle.sql.CLOB || rs.getObject(i+1) instanceof java.sql.Clob ){
							try {
								if(rs.getClob(i+1)!=null){
									objs[i]=StringManagerUtils.CLOBtoString2(rs.getClob(i+1));
								}else{
									objs[i]="";
								}
							} catch (IOException e) {
								objs[i]="";
								e.printStackTrace();
							}
						}else{
							objs[i]=rs.getObject(i+1);
						}
					}
					list.add(objs);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(sql);
		}finally{
			OracleJdbcUtis.closeDBConnection(conn, ps, rs);
		}
		return list;
	}
	
	public static List<Object[]> query(Connection conn,PreparedStatement ps,ResultSet rs,String sql) {
		ResultSetMetaData rsmd=null;
		List<Object[]> list=new ArrayList<Object[]>();
		
		try {
			ps = conn.prepareStatement(sql);
			rs=ps.executeQuery();
			rsmd=rs.getMetaData();  
			int columnCount=rsmd.getColumnCount();  
			while(rs.next())
			{
				Object[] objs=new Object[columnCount];
				for(int i=0;i<columnCount;i++){
					objs[i]=rs.getObject(i+1);
				}
				list.add(objs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(sql);
		}
		return list;
	}
	
	public static List<Object[]> query(Connection conn,PreparedStatement ps,ResultSet rs,String sql,int timeout) {
		ResultSetMetaData rsmd=null;
		List<Object[]> list=new ArrayList<Object[]>();
		
		try {
			ps = conn.prepareStatement(sql);
			ps.setQueryTimeout(timeout);
			rs=ps.executeQuery();
			rsmd=rs.getMetaData();  
			int columnCount=rsmd.getColumnCount();  
			while(rs.next())
			{
				Object[] objs=new Object[columnCount];
				for(int i=0;i<columnCount;i++){
					objs[i]=rs.getObject(i+1);
				}
				list.add(objs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(sql);
		}
		return list;
	}
}
