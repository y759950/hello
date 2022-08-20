package dbpool;

/**
 * Title:        yika数据库连接池
 * Description:  数据库连接池
 * Copyright:    Copyright (c) 2001
 * Company:      苏州市yika软件业服务有限公司
 * @author yika
 * @version 1.3
 *
 * 修改记录：1. 2003-12-08 蒋智湘 更改数据库配置文件的位置判定。具体参见void init()方法
 *    2、2005-1-19 蒋智湘 为了能在oracle数据库中使用日期字符串，
 *       在配置文件中增加了配置属性：sessionConfigStatement
 *    3、2005-3-25 蒋智湘 从配置文件中获取密码后，如果在System中定义了解密接口实现类，
 *       则对密码进行解密。
 */
import java.io.*;
import java.sql.*;
import java.util.*;

import com.zcsoft.dbpool.ConnectionPool;
import com.zcsoft.log.*;
/**
 * 连接池管理类，用于管理多个连接池。此类也是外部接口类
 * 数据库连接配置文件默认为该类文件相同目录下的"db.properties"，如果该文件
 * 不存在，则试图使用C:\zcsoft\db.properties文件，如果该文件还是不存在，则
 * 在操作系统当前用户目录下查找db.properties文件，如果还失败，则抛出异常
 *
 */
public class PoolManager
{

	//类实例，一个虚拟机中只需一个实例否则就失去了使用连接池的意义
	static private PoolManager instance;
	//此实例被调用的个数
	static private int clients;

	//日志类
	private LogWriter logWriter;
	//输出流
	private PrintWriter pw;
	//配置文件路径，此处为绝对路径
	private static String initFilePath = "db.properties";

	//数据库连接驱动器容器
	private Vector drivers = new Vector();
	//连接池容器
	private Hashtable pools = new Hashtable();

	//此构造函数为私有，不可直接生成对象实例
	private PoolManager()
	{
		init();
	}

	/**
	 * 静态方法，用于外部接口。以获取管理类实例
	 * @return PoolManager 连接池管理类实例
	 */
	static synchronized public PoolManager getInstance(String initFP)
	{
		//只有第一次使用时，initFP才可能被使用
		if (instance == null)
		{
			if(initFP != null)
			{
				initFilePath = initFP;
			}
			instance = new PoolManager();
		}
		clients++;
		return instance;
	}

	/**
	 * 静态方法，用于外部接口。以获取管理类实例
	 * @return PoolManager 连接池管理类实例
	 */
	static synchronized public PoolManager getInstance()
	{
		return getInstance(null);
	}

	/**
	 * 初始化此类
	 */
	private void init()
	{
		// Log to System.err until we have read the logfile property
		pw = new PrintWriter(System.err, true);
		logWriter = new LogWriter("PoolManager", LogWriter.INFO, pw);
		Properties dbProps = new Properties();
		try
		{
			String fileName = initFilePath;
			//优先在该类文件所在目录下找
			InputStream is = null;
			if(fileName.indexOf(File.separatorChar) == -1
				&& fileName.indexOf('/') == -1)
			{
				is = getClass().getResourceAsStream(fileName);
				if(is == null)//没有找到，就在C:/zcsoft/下找
				{
					File f = new File("C:/zcsoft/" + fileName);
					if (f.exists()) //如果还是没有，则在用户目录下找
					{
						is = new FileInputStream(f);
					}
					else
					{
						is = new FileInputStream(System.getProperty("user.home") + File.separator + fileName);
					}
				}
			}
			else//为绝对路径
			{
				is = new FileInputStream(fileName);
			}
			dbProps.load(is);
		}
		catch(Exception ex)
		{
			logWriter.log(ex, "", LogWriter.ERROR);
			return;
		}
		//设置日志文件路径，并将日志输出流重定向
		String logFile = dbProps.getProperty("logfile");
		if (logFile != null)
		{
			try
			{
				pw = new PrintWriter(new FileWriter(logFile, true), true);
				logWriter.setPrintWriter(pw);
			}
			catch (IOException e)
			{
				logWriter.log("Can't open the log file: " + logFile +
								  ". Using System.err instead", LogWriter.ERROR);
			}
		}
		//加载数据库驱动器
		loadDrivers(dbProps);
		//创建连接池
		createPools(dbProps);
	}

	/**
	 * 加载数据库驱动器
	 * 再属性drivers中各驱动器以空格分割
	 * @param props 属性文件
	 */
	private void loadDrivers(Properties props)
	{
		String driverClasses = props.getProperty("drivers");
		StringTokenizer st = new StringTokenizer(driverClasses);
		while (st.hasMoreElements())
		{
			String driverClassName = st.nextToken().trim();
			try
			{
				Driver driver = (Driver)
						  Class.forName(driverClassName).newInstance();
				DriverManager.registerDriver(driver);
				drivers.addElement(driver);
				logWriter.log("Registered JDBC driver " + driverClassName,
								  LogWriter.INFO);
			}
			catch (Exception e)
			{
				logWriter.log(e, "Can't register JDBC driver: " +
								  driverClassName, LogWriter.ERROR);
			}
		}
	}

	/**
	 * 建立连接池
	 * @param props 属性文件
	 */
	private void createPools(Properties props)
	{
		/**读取属性文件的属性名：(=前面的字符串)*/
		Enumeration propNames = props.propertyNames();
		while (propNames.hasMoreElements())
		{
			String name = (String) propNames.nextElement();
			//连接池中url参数为必选项，否则此连接池名将被忽略，而且url参数必须排在
			//其它属性的前面否则前面的参数也将被忽略
			if (name.endsWith(".url"))
			{
				//获取连接池名
				String poolName = name.substring(0, name.lastIndexOf("."));
				//获取数据库连接路径
				String url = props.getProperty(poolName + ".url");
				//如url为null则跳转
				if (url == null)
				{
					logWriter.log("No URL specified for " + poolName,
									  LogWriter.ERROR);
					continue;
				}
				//获取用户名
				String user = props.getProperty(poolName + ".user");
				//获取密码
				String password = props.getProperty(poolName + ".password");
				//2005-3-25 蒋智湘
				String encryptorClass;
				if ((encryptorClass = System.getProperty("db.config.encryptor")) != null)
				{
					try
					{
						Class c = Class.forName(encryptorClass);
						com.zcsoft.security.Encryptor encryptor = (com.zcsoft.security.Encryptor)c.newInstance();
						password = encryptor.dencrypt(password);
					}
					catch (Exception ex)
					{
						System.out.println("when instantiate encryptor class " + encryptorClass
												 + ", caught exception:" + ex);
					}
				}
				//获取最大连接数，默认为0
				String maxConns = props.getProperty(poolName +
						".maxconns", "0");
				int max;
				try
				{
					max = Integer.valueOf(maxConns).intValue();
				}
				catch (NumberFormatException e)
				{
					logWriter.log("Invalid maxconns value " + maxConns +
									  " for " + poolName, LogWriter.ERROR);
					max = 0;
				}

				//获取初始化连接数，默认为0
				String initConns = props.getProperty(poolName +
						".initconns", "0");
				int init;
				try
				{
					init = Integer.valueOf(initConns).intValue();
				}
				catch (NumberFormatException e)
				{
					logWriter.log("Invalid initconns value " + initConns +
									  " for " + poolName, LogWriter.ERROR);
					init = 0;
				}

				//获取超时数，此数单位为秒，默认为5
				String loginTimeOut = props.getProperty(poolName +
						".logintimeout", "5");
				int timeOut;
				try
				{
					timeOut = Integer.valueOf(loginTimeOut).intValue();
				}
				catch (NumberFormatException e)
				{
					logWriter.log("Invalid logintimeout value " + loginTimeOut +
									  " for " + poolName, LogWriter.ERROR);
					timeOut = 5;
				}

				//获取日志级别，默认为错误级数为2
				String logLevelProp = props.getProperty(poolName +
						".loglevel",LogWriter.INFO_TEXT);
				int logLevel = LogWriter.INFO;
				if (logLevelProp.equalsIgnoreCase(LogWriter.NONE_TEXT))
				{
					logLevel = LogWriter.NONE;
				}
				else if (logLevelProp.equalsIgnoreCase(LogWriter.ERROR_TEXT))
				{
					logLevel = LogWriter.ERROR;
				}
				else if (logLevelProp.equalsIgnoreCase(LogWriter.INFO_TEXT))
				{
					logLevel = LogWriter.INFO;
				}
				else if (logLevelProp.equalsIgnoreCase(LogWriter.DEBUG_TEXT))
				{
					logLevel = LogWriter.DEBUG;
				}
				String sessionConfigStmt = props.getProperty(poolName + ".sessionConfigStatement");
				//按获取的参数建立连接池实例
				ConnectionPool pool =
						new ConnectionPool(poolName, url, user, password,
						max, init, timeOut, pw, logLevel, sessionConfigStmt);
				//将新建的连接池实例加入容器中。以hash表保存。
				pools.put(poolName, pool);
			}
		}
	}

	/**
	 * 按所给的连接池名获取此连接池中的连接
	 * @param name 连接池名
	 * @return Connection 获取的连接
	 */
	public Connection getConnection(String name)
	{
		Connection conn = null;
		ConnectionPool pool = (ConnectionPool) pools.get(name);
		if (pool != null)
		{
			try
			{
				conn = pool.getConnection();
			}
			catch (SQLException e)
			{
				logWriter.log(e, "Exception getting connection from " +
								  name, LogWriter.ERROR);
			}
		}
		return conn;
	}

	/**
	 * 释放连接到指定的连接池中
	 * @param name 连接池名
	 * @param con 要放回的连接
	 */
	public void freeConnection(String name, Connection con)
	{
		ConnectionPool pool = (ConnectionPool) pools.get(name);
		if (pool != null)
		{
			pool.freeConnection(con);
		}
	}

	/**
	 * 释放所有连接池中的所有连接和数据库驱动器
	 */
	public synchronized void release()
	{
		// Wait until called by the last client
		if (--clients != 0)
		{
			return;
		}

		Enumeration allPools = pools.elements();
		while (allPools.hasMoreElements())
		{
			ConnectionPool pool = (ConnectionPool) allPools.nextElement();
			pool.release();
		}

		Enumeration allDrivers = drivers.elements();
		while (allDrivers.hasMoreElements())
		{
			Driver driver = (Driver) allDrivers.nextElement();
			try
			{
				DriverManager.deregisterDriver(driver);
				logWriter.log("Deregistered JDBC driver " +
								  driver.getClass().getName(), LogWriter.INFO);
			}
			catch (SQLException e)
			{
				logWriter.log(e, "Couldn't deregister JDBC driver: " +
								  driver.getClass().getName(), LogWriter.ERROR);
			}
		}
	}

	/**
  public static void main(String[] args)
  {
	 PoolManager pm = PoolManager.getInstance();
	 String poolName = "db1";
	 pm.freeConnection(poolName,pm.getConnection(poolName));
	 Connection[] cons = new Connection[]{null,null,null,null,null};
	 cons[0] = pm.getConnection(poolName);
	 cons[1] = pm.getConnection(poolName);
	 cons[2] = pm.getConnection(poolName);
	 cons[3] = pm.getConnection(poolName);
	 pm.freeConnection(poolName,cons[1]);
	 cons[3] = pm.getConnection(poolName);
	 pm.freeConnection(poolName,cons[0]);
	 pm.freeConnection(poolName,cons[3]);
	 //pm.freeConnection(poolName,cons[2]);
	 pm.release();
	 try
	 {
	 cons[2].close();
	 }
	 catch(SQLException se)
	 {se.printStackTrace();}
	 finally
	 {
	 System.out.println("in finally");
	 }
	 System.out.println("out of try catch struct");
  }//*/
}