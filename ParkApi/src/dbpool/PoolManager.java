package dbpool;

/**
 * Title:        yika���ݿ����ӳ�
 * Description:  ���ݿ����ӳ�
 * Copyright:    Copyright (c) 2001
 * Company:      ������yika���ҵ�������޹�˾
 * @author yika
 * @version 1.3
 *
 * �޸ļ�¼��1. 2003-12-08 ������ �������ݿ������ļ���λ���ж�������μ�void init()����
 *    2��2005-1-19 ������ Ϊ������oracle���ݿ���ʹ�������ַ�����
 *       �������ļ����������������ԣ�sessionConfigStatement
 *    3��2005-3-25 ������ �������ļ��л�ȡ����������System�ж����˽��ܽӿ�ʵ���࣬
 *       ���������н��ܡ�
 */
import java.io.*;
import java.sql.*;
import java.util.*;

import com.zcsoft.dbpool.ConnectionPool;
import com.zcsoft.log.*;
/**
 * ���ӳع����࣬���ڹ��������ӳء�����Ҳ���ⲿ�ӿ���
 * ���ݿ����������ļ�Ĭ��Ϊ�����ļ���ͬĿ¼�µ�"db.properties"��������ļ�
 * �����ڣ�����ͼʹ��C:\zcsoft\db.properties�ļ���������ļ����ǲ����ڣ���
 * �ڲ���ϵͳ��ǰ�û�Ŀ¼�²���db.properties�ļ��������ʧ�ܣ����׳��쳣
 *
 */
public class PoolManager
{

	//��ʵ����һ���������ֻ��һ��ʵ�������ʧȥ��ʹ�����ӳص�����
	static private PoolManager instance;
	//��ʵ�������õĸ���
	static private int clients;

	//��־��
	private LogWriter logWriter;
	//�����
	private PrintWriter pw;
	//�����ļ�·�����˴�Ϊ����·��
	private static String initFilePath = "db.properties";

	//���ݿ���������������
	private Vector drivers = new Vector();
	//���ӳ�����
	private Hashtable pools = new Hashtable();

	//�˹��캯��Ϊ˽�У�����ֱ�����ɶ���ʵ��
	private PoolManager()
	{
		init();
	}

	/**
	 * ��̬�����������ⲿ�ӿڡ��Ի�ȡ������ʵ��
	 * @return PoolManager ���ӳع�����ʵ��
	 */
	static synchronized public PoolManager getInstance(String initFP)
	{
		//ֻ�е�һ��ʹ��ʱ��initFP�ſ��ܱ�ʹ��
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
	 * ��̬�����������ⲿ�ӿڡ��Ի�ȡ������ʵ��
	 * @return PoolManager ���ӳع�����ʵ��
	 */
	static synchronized public PoolManager getInstance()
	{
		return getInstance(null);
	}

	/**
	 * ��ʼ������
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
			//�����ڸ����ļ�����Ŀ¼����
			InputStream is = null;
			if(fileName.indexOf(File.separatorChar) == -1
				&& fileName.indexOf('/') == -1)
			{
				is = getClass().getResourceAsStream(fileName);
				if(is == null)//û���ҵ�������C:/zcsoft/����
				{
					File f = new File("C:/zcsoft/" + fileName);
					if (f.exists()) //�������û�У������û�Ŀ¼����
					{
						is = new FileInputStream(f);
					}
					else
					{
						is = new FileInputStream(System.getProperty("user.home") + File.separator + fileName);
					}
				}
			}
			else//Ϊ����·��
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
		//������־�ļ�·����������־������ض���
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
		//�������ݿ�������
		loadDrivers(dbProps);
		//�������ӳ�
		createPools(dbProps);
	}

	/**
	 * �������ݿ�������
	 * ������drivers�и��������Կո�ָ�
	 * @param props �����ļ�
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
	 * �������ӳ�
	 * @param props �����ļ�
	 */
	private void createPools(Properties props)
	{
		/**��ȡ�����ļ�����������(=ǰ����ַ���)*/
		Enumeration propNames = props.propertyNames();
		while (propNames.hasMoreElements())
		{
			String name = (String) propNames.nextElement();
			//���ӳ���url����Ϊ��ѡ���������ӳ����������ԣ�����url������������
			//�������Ե�ǰ�����ǰ��Ĳ���Ҳ��������
			if (name.endsWith(".url"))
			{
				//��ȡ���ӳ���
				String poolName = name.substring(0, name.lastIndexOf("."));
				//��ȡ���ݿ�����·��
				String url = props.getProperty(poolName + ".url");
				//��urlΪnull����ת
				if (url == null)
				{
					logWriter.log("No URL specified for " + poolName,
									  LogWriter.ERROR);
					continue;
				}
				//��ȡ�û���
				String user = props.getProperty(poolName + ".user");
				//��ȡ����
				String password = props.getProperty(poolName + ".password");
				//2005-3-25 ������
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
				//��ȡ�����������Ĭ��Ϊ0
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

				//��ȡ��ʼ����������Ĭ��Ϊ0
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

				//��ȡ��ʱ����������λΪ�룬Ĭ��Ϊ5
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

				//��ȡ��־����Ĭ��Ϊ������Ϊ2
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
				//����ȡ�Ĳ����������ӳ�ʵ��
				ConnectionPool pool =
						new ConnectionPool(poolName, url, user, password,
						max, init, timeOut, pw, logLevel, sessionConfigStmt);
				//���½������ӳ�ʵ�����������С���hash���档
				pools.put(poolName, pool);
			}
		}
	}

	/**
	 * �����������ӳ�����ȡ�����ӳ��е�����
	 * @param name ���ӳ���
	 * @return Connection ��ȡ������
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
	 * �ͷ����ӵ�ָ�������ӳ���
	 * @param name ���ӳ���
	 * @param con Ҫ�Żص�����
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
	 * �ͷ��������ӳ��е��������Ӻ����ݿ�������
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