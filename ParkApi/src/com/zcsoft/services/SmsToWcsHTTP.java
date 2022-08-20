package com.zcsoft.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zcsoft.dbvisit.DB;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class SmsToWcsHTTP extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmsToWcsHTTP.class);
	/**���ݿ���ʲ���ʵ��*/
    static DB db=new DB();
    
    
    
    /**
	 * ��ʼ����
	 * @param sc
	 * ServletException
	 */
	public void init(ServletConfig sc) throws ServletException
	{
		super.init(sc);
		outInfo(log, "serverCenter init");
		

		
		
	}
	
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
    	doPost(request,response);
    	//System.out.println("this is doGet:"+request);
     }
   
     protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
	 		
    	// ��ȡ�����URI��ַ��Ϣ
         String url = request.getRequestURI();
         // ��ȡ���еķ�����
         String methodName = url.substring(url.lastIndexOf("/")+1);
         Method method = null;
         try {
             // ʹ�÷�����ƻ�ȡ�ڱ����������˵ķ���
             method = getClass().getDeclaredMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
             // ִ�з���
             Object msg = method.invoke(this, request, response);
             response.setCharacterEncoding("utf-8");
             response.setContentType("text/html;charset=utf-8");
             PrintWriter out = response.getWriter();
             
             
    		 out.print(msg.toString());//������Ϣ
    		 out.close();
         } catch (Exception e) {
             throw new RuntimeException("���÷�������");
         }
         
         
	 		
     }
     
     public  String getParams(HttpServletRequest request) {
    	 String params = "";

    	 try {
 	    	BufferedReader br;
  			br = new BufferedReader(new InputStreamReader(request.getInputStream()));
  			String line = null;
 		    StringBuilder sb = new StringBuilder();
 		    while((line = br.readLine())!=null){
 		      sb.append(line);
 		    }
 		    params =sb.toString();
 		 } catch (Exception e) {
 			 // TODO: handle exception
 		 }
    	 return     params ;

     }
     
     /**
      * 
      * @param request
      * @param response
      * @throws ServletException
      * @throws IOException
      */
     private String sendLocationStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 Vector sqls = new Vector(2);
 	     Vector values = new Vector(2);
    	 String params = getParams(request);
    	 JSONObject result = new JSONObject();
    	 result.element("success",false);
         result.element("errorMsg","");

    	
	     try {
	    	 
	    	 outInfo(log, "sendLocationStatus"+params);
    	    /**ʹ��JSONObject����*/
    		JSONObject jsonObject = JSONObject.fromObject(params);
    
//    			 ��λ��� ��+��+��
//    			 ����(01 - 20)
//    			 ������1�����У�2������
//    			 ����(01 - 20)
//    			 "״̬��DISABLE, EMPTY, FULL, PENDING_IN, PENDING_OUT
//    			 �����ã����У����ã�����ʹ�ã��ȴ���⣬�ȴ�����"
//    			 SPS��ID,�����

    	  	  String  unitId = jsonObject.getString("unitId");
    	  	  String  rowIndex =  jsonObject.getString("rowIndex");
    	  	  String  columnIndex = jsonObject.getString("columnIndex");
    	  	  String  layerIndex = jsonObject.getString("layerIndex");
    	  	  String  status =  jsonObject.getString("status");
    	  	  String  spsId = jsonObject.getString("spsId");

    	  	  String id=com.zcsoft.util.SerialNumber.getSerialNumber("sendLocationStatus_id");
    		sqls.add("insert  into sendLocationStatus(id,unitId,rowIndex,columnIndex,layerIndex"
    				+ ",status,spsId,zt,bz,czsj)values(?,?,?,?,?, ?,?,?,?,getdate())");
    		values.add(new Object[] {id,unitId,rowIndex,columnIndex,layerIndex,status,spsId
    				,"0",null});
    		  
   
        	return result.toString();	  
    	  	
  		} catch (JSONException e) {
            result.element("success",false);
              result.element("errorMsg","����ʧ�ܣ�JSON����");
              log.error("sendLocationStatus�ӿڴ��룺"+params+"������"+result.toString());
          	return result.toString();
  		}catch (IllegalArgumentException e) {
            result.element("success",false);
              result.element("errorMsg","����ʧ�ܣ�SQL����");
              log.error("sendLocationStatus�ӿڴ��룺"+params+"������"+result.toString());
          	return result.toString();
  		}catch (Exception e) {
            result.element("success",false);
              result.element("errorMsg","����ʧ�ܣ�");
              log.error("sendLocationStatus�ӿڴ��룺"+params+"������"+result.toString());
          	return result.toString();
  		}
	     
     }

     private String sendAGVTaskStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 Vector sqls = new Vector(2);
 	     Vector values = new Vector(2);
    	 String params = getParams(request);
    	 JSONObject result = new JSONObject();
    	 result.element("success",false);
         result.element("errorMsg","");

    	
	     try {
	    	 
	    	 outInfo(log, "sendAGVTaskStatus"+params);
    	    /**ʹ��JSONObject����*/
    		JSONObject jsonObject = JSONObject.fromObject(params);
    
//    		stationIndex	int	����ڱ�ţ�1��2
//    		taskId	String	�����
//    		status	String	"����״̬��PENDING, EXECUTING, AGV_TAKEN, FINISHED, ERROR
//    		��ִ�У�ִ���У�AGV��ȡ�ߣ�����ɣ��쳣����"
//    		errorCause	String	�쳣ԭ��

    		JSONArray  jsonary=JSONArray.fromObject(jsonObject);
    		
    		for(int i=0;i<jsonary.size();i++) {
    			JSONObject	jsonObject1 =jsonary.getJSONObject(i);
    			
    			String  stationIndex = jsonObject1.getString("stationIndex");
    			String  taskId = jsonObject1.getString("taskId");
    			String  status =jsonObject1. getString("status");
    			String  errorCause = jsonObject1.getString("errorCause");
    			
    			String id=com.zcsoft.util.SerialNumber.getSerialNumber("sendAGVTaskStatus_id");
    			sqls.add("insert  into sendAGVTaskStatus(id,stationIndex,taskId,status,"
    					+ "errorCause"
    					+ ",zt,bz,czsj)values(?,?,?,?,?, ?,?,getdate())");
    			values.add(new Object[] {id,stationIndex,taskId,status,errorCause
    					,"0",null});
    		}
    		  
   
        	return result.toString();	  
    	  	
  		} catch (JSONException e) {
            result.element("success",false);
              result.element("errorMsg","����ʧ�ܣ�JSON����");
              log.error("sendAGVTaskStatus�ӿڴ��룺"+params+"������"+result.toString());
          	return result.toString();
  		}catch (IllegalArgumentException e) {
            result.element("success",false);
              result.element("errorMsg","����ʧ�ܣ�SQL����");
              log.error("sendAGVTaskStatus�ӿڴ��룺"+params+"������"+result.toString());
          	return result.toString();
  		}catch (Exception e) {
            result.element("success",false);
              result.element("errorMsg","����ʧ�ܣ�");
              log.error("sendAGVTaskStatus�ӿڴ��룺"+params+"������"+result.toString());
          	return result.toString();
  		}
	     
     }
     
     
     private String GetRKLocation(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 Vector sqls = new Vector(2);
    	 Vector values = new Vector(2);
    	 String params = getParams(request);
    	 JSONObject result = new JSONObject();
    	 result.element("success",false);
    	 result.element("errorMsg","");
    	 
    	 
    	 try {
    		 
    		 outInfo(log, "GetRKLocation"+params);
    		 /**ʹ��JSONObject����*/
    		 JSONObject jsonObject = JSONObject.fromObject(params);
    		 
//    		 spsId	String	SPS��ID 

    		
    			 String  spsId = jsonObject.getString("spsId");

    			 String id=com.zcsoft.util.SerialNumber.getSerialNumber("GetRKLocation_id");
    			 sqls.add("insert  into GetRKLocation(id,"
    					 + "spsId"
    					 + ",zt,bz,czsj)values(?,?,?,?,getdate())");
    			 values.add(new Object[] {id,spsId,
    					 "0",null});
    		 
    		 
    		 
    		 return result.toString();	  
    		 
    	 } catch (JSONException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�JSON����");
    		 log.error("GetRKLocation�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }catch (IllegalArgumentException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�SQL����");
    		 log.error("GetRKLocation�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }catch (Exception e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�");
    		 log.error("GetRKLocation�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }
    	 
     }
     private String GetDeviceStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 Vector sqls = new Vector(2);
    	 Vector values = new Vector(2);
    	 String params = getParams(request);
    	 JSONObject result = new JSONObject();
    	 result.element("success",false);
    	 result.element("errorMsg","");
    	 
    	 
    	 try {
    		 
    		 outInfo(log, "GetDeviceStatus"+params);
    		 /**ʹ��JSONObject����*/
    		 JSONObject jsonObject = JSONObject.fromObject(params);
    		 
//    		 spsId	String	SPS��ID 
    		 
    		 
    		 String  status = jsonObject.getString("status");
    		 
    		 String id=com.zcsoft.util.SerialNumber.getSerialNumber("GetDeviceStatus_id");
    		 sqls.add("insert  into GetDeviceStatus(id,"
    				 + "status"
    				 + ",zt,bz,czsj)values(?,?,?,?,getdate())");
    		 values.add(new Object[] {id,status,
    				 "0",null});
    		 
    		 
    		 
    		 return result.toString();	  
    		 
    	 } catch (JSONException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�JSON����");
    		 log.error("GetDeviceStatus�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }catch (IllegalArgumentException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�SQL����");
    		 log.error("GetDeviceStatus�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }catch (Exception e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�");
    		 log.error("GetDeviceStatus�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }
    	 
     }
     
     
     
     private String sendTaskStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 Vector sqls = new Vector(2);
    	 Vector values = new Vector(2);
    	 String params = getParams(request);
    	 JSONObject result = new JSONObject();
    	 result.element("success",false);
    	 result.element("errorMsg","");
    	 
    	 
    	 try {
    		 
    		 outInfo(log, "sendTaskStatus"+params);
    		 /**ʹ��JSONObject����*/
    		 JSONObject jsonObject = JSONObject.fromObject(params);
    		 
//    		 taskId	String	�����
//    		 status	String	"����״̬��PENDING, EXECUTING, AGV_TAKEN, FINISHED, MANUAL_FINISHED, ERROR
//    		 ��ִ�У�ִ���У�AGV��ȡ�ߣ�����ɣ��ֶ���ɣ��쳣����"
//    		 taskType	String	�������ͣ�IN ��⣬OUT ����
//    		 errorCause	String	�쳣ԭ��

    		 
    		 JSONArray  jsonary=JSONArray.fromObject(jsonObject);
    		 
    		 for(int i=0;i<jsonary.size();i++) {
    			 JSONObject	jsonObject1 =jsonary.getJSONObject(i);
    			 
    			 String  taskType = jsonObject1.getString("taskType");
    			 String  taskId = jsonObject1.getString("taskId");
    			 String  status =jsonObject1. getString("status");
    			 String  errorCause = jsonObject1.getString("errorCause");
    			 
    			 String id=com.zcsoft.util.SerialNumber.getSerialNumber("sendTaskStatus_id");
    			 sqls.add("insert  into sendTaskStatus(id,taskType,taskId,status,"
    					 + "errorCause"
    					 + ",zt,bz,czsj)values(?,?,?,?,?, ?,?,getdate())");
    			 values.add(new Object[] {id,taskType,taskId,status,errorCause
    					 ,"0",null});
    		 }
    		 
    		 
    		 return result.toString();	  
    		 
    	 } catch (JSONException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�JSON����");
    		 log.error("sendTaskStatus�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }catch (IllegalArgumentException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�SQL����");
    		 log.error("sendTaskStatus�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }catch (Exception e) {
    		 result.element("success",false);
    		 result.element("errorMsg","����ʧ�ܣ�");
    		 log.error("sendTaskStatus�ӿڴ��룺"+params+"������"+result.toString());
    		 return result.toString();
    	 }
    	 
     }
		
     /**
 	 * ��� ��־
 	 * */
 	public void outDebug(org.slf4j.Logger logger, String msg)
 	{
 		logger.debug(msg);
 		System.out.println(msg);
 	}
 	public void outInfo(org.slf4j.Logger logger, String msg)
 	{
 		logger.info(msg);
 		System.out.println(msg);
 	}
 	public void outError(org.slf4j.Logger logger, String msg)
 	{
 		logger.error(msg);
 		System.out.println(msg);
 	}
}
