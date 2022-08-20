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
	/**数据库访问操作实例*/
    static DB db=new DB();
    
    
    
    /**
	 * 初始设置
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
	 		
    	// 获取请求的URI地址信息
         String url = request.getRequestURI();
         // 截取其中的方法名
         String methodName = url.substring(url.lastIndexOf("/")+1);
         Method method = null;
         try {
             // 使用反射机制获取在本类中声明了的方法
             method = getClass().getDeclaredMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
             // 执行方法
             Object msg = method.invoke(this, request, response);
             response.setCharacterEncoding("utf-8");
             response.setContentType("text/html;charset=utf-8");
             PrintWriter out = response.getWriter();
             
             
    		 out.print(msg.toString());//返回信息
    		 out.close();
         } catch (Exception e) {
             throw new RuntimeException("调用方法出错！");
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
    	    /**使用JSONObject解析*/
    		JSONObject jsonObject = JSONObject.fromObject(params);
    
//    			 储位编号 排+列+层
//    			 排数(01 - 20)
//    			 列数，1是外列，2是内列
//    			 层数(01 - 20)
//    			 "状态：DISABLE, EMPTY, FULL, PENDING_IN, PENDING_OUT
//    			 不可用，空闲（可用），已使用，等待入库，等待出库"
//    			 SPS框ID,条码号

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
              result.element("errorMsg","操作失败，JSON有误！");
              log.error("sendLocationStatus接口传入："+params+"传出："+result.toString());
          	return result.toString();
  		}catch (IllegalArgumentException e) {
            result.element("success",false);
              result.element("errorMsg","操作失败，SQL有误！");
              log.error("sendLocationStatus接口传入："+params+"传出："+result.toString());
          	return result.toString();
  		}catch (Exception e) {
            result.element("success",false);
              result.element("errorMsg","操作失败！");
              log.error("sendLocationStatus接口传入："+params+"传出："+result.toString());
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
    	    /**使用JSONObject解析*/
    		JSONObject jsonObject = JSONObject.fromObject(params);
    
//    		stationIndex	int	出库口编号：1，2
//    		taskId	String	任务号
//    		status	String	"任务状态：PENDING, EXECUTING, AGV_TAKEN, FINISHED, ERROR
//    		待执行，执行中，AGV已取走，已完成，异常出错"
//    		errorCause	String	异常原因

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
              result.element("errorMsg","操作失败，JSON有误！");
              log.error("sendAGVTaskStatus接口传入："+params+"传出："+result.toString());
          	return result.toString();
  		}catch (IllegalArgumentException e) {
            result.element("success",false);
              result.element("errorMsg","操作失败，SQL有误！");
              log.error("sendAGVTaskStatus接口传入："+params+"传出："+result.toString());
          	return result.toString();
  		}catch (Exception e) {
            result.element("success",false);
              result.element("errorMsg","操作失败！");
              log.error("sendAGVTaskStatus接口传入："+params+"传出："+result.toString());
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
    		 /**使用JSONObject解析*/
    		 JSONObject jsonObject = JSONObject.fromObject(params);
    		 
//    		 spsId	String	SPS框ID 

    		
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
    		 result.element("errorMsg","操作失败，JSON有误！");
    		 log.error("GetRKLocation接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }catch (IllegalArgumentException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","操作失败，SQL有误！");
    		 log.error("GetRKLocation接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }catch (Exception e) {
    		 result.element("success",false);
    		 result.element("errorMsg","操作失败！");
    		 log.error("GetRKLocation接口传入："+params+"传出："+result.toString());
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
    		 /**使用JSONObject解析*/
    		 JSONObject jsonObject = JSONObject.fromObject(params);
    		 
//    		 spsId	String	SPS框ID 
    		 
    		 
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
    		 result.element("errorMsg","操作失败，JSON有误！");
    		 log.error("GetDeviceStatus接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }catch (IllegalArgumentException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","操作失败，SQL有误！");
    		 log.error("GetDeviceStatus接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }catch (Exception e) {
    		 result.element("success",false);
    		 result.element("errorMsg","操作失败！");
    		 log.error("GetDeviceStatus接口传入："+params+"传出："+result.toString());
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
    		 /**使用JSONObject解析*/
    		 JSONObject jsonObject = JSONObject.fromObject(params);
    		 
//    		 taskId	String	任务号
//    		 status	String	"任务状态：PENDING, EXECUTING, AGV_TAKEN, FINISHED, MANUAL_FINISHED, ERROR
//    		 待执行，执行中，AGV已取走，已完成，手动完成，异常出错"
//    		 taskType	String	任务类型：IN 入库，OUT 出库
//    		 errorCause	String	异常原因

    		 
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
    		 result.element("errorMsg","操作失败，JSON有误！");
    		 log.error("sendTaskStatus接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }catch (IllegalArgumentException e) {
    		 result.element("success",false);
    		 result.element("errorMsg","操作失败，SQL有误！");
    		 log.error("sendTaskStatus接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }catch (Exception e) {
    		 result.element("success",false);
    		 result.element("errorMsg","操作失败！");
    		 log.error("sendTaskStatus接口传入："+params+"传出："+result.toString());
    		 return result.toString();
    	 }
    	 
     }
		
     /**
 	 * 输出 日志
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
