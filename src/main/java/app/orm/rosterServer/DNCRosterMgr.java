package app.orm.rosterServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;

import app.orm.rosterServer.mybatis.DBProxy;
import component.orm.QueryCondition;
import component.orm.protocol.QueryResult;
import component.orm.protocol.Request;
/**
 * DNC类
 * 
 * @author wangjun
 * @version 1.0
 *
 */
import component.orm.protocol.Response;
import component.orm.protocol.Result;
import component.util.Util;
public class DNCRosterMgr {
	
	private DBProxy dncRosterDao = Util.getBean("dbProxy", DBProxy.class);
	private List<Object> dncPhoneNumList = null;
	
	public Response processRequest(Request req){
		Util.info(this, "DNCRosterMgr ----------- processRequest");
		Response response = new Response();
		if (req.getMethod().equals("queryDNCRoster")){
			Util.info(this, "DNCRosterMgr - processRequest - queryDNCRoster");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			QueryResult queryResult = new QueryResult();
			
			Gson gson = new Gson();
			Object obj = req.getParams();
			String json = gson.toJson(obj);
			
			QueryCondition query = gson.fromJson(json, QueryCondition.class);
			
			int curPage = query.getCurPage();
			int pageNum = query.getPageNum();
			
			Util.info(this, "curPage:" + curPage);
			if (curPage <= 0) {
				queryResult.setCode(0);
				queryResult.setReason("Query success!");
				queryResult.setResList(dncPhoneNumList);
			} else {

				int beginIndex = (curPage - 1) * pageNum + 1;
				int endIndex = curPage * pageNum;
				int size = this.dncPhoneNumList.size();
				int pageCount = 0;
				if (size % pageNum == 0) {
					pageCount = size / pageNum;
				} else {
					pageCount = size / pageNum + 1;
				}

				queryResult.setCurPage(curPage);
				queryResult.setCount(size);
				queryResult.setPageCount(pageCount);
				if (beginIndex > size) {
					queryResult.setCode(-1);
					queryResult.setReason("Beyond Max Size!");
				} else if (beginIndex <= size && endIndex > size) {
					queryResult.setCode(0);
					queryResult.setReason("Query success!");
					queryResult.setResList(dncPhoneNumList.subList(beginIndex - 1,
							size));
				} else if (endIndex <= size) {
					queryResult.setCode(0);
					queryResult.setReason("Query success!");
					queryResult.setResList(dncPhoneNumList.subList(beginIndex - 1,
							endIndex));
				}
			}
			response.setResult(queryResult);
		} else if (req.getMethod().equals("delete")) {
			Util.info(this, "DNCRosterMgr - processRequest - delete");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Gson gson = new Gson();
			Object obj = req.getParams();
			String json = gson.toJson(obj);

			PhoneNumber map = gson.fromJson(json, PhoneNumber.class);

			String phoneNum = map.getPhoneNum();
			Util.info(this, "DNCRosterMgr - processRequest - delete phoneNum: %s", phoneNum);
			boolean flag = deletePhoneNum(phoneNum);
			if (flag) {
				result.setCode(0);
				result.setReason("Delete phoneNum success!");
			} else {
				result.setCode(-1);
				result.setReason("Delete phoneNum failure!");
			}

			initForm();// 重新读取Policylist信息
			response.setResult(result);
		} else {
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");

			Result result = new Result();
			result.setCode(-1);
			result.setReason("Unknow Method!");
			response.setResult(result);
		}
		
		return response;
	}
	
	
	public boolean loadDNCFileContentIntoDB(String path) {
		
		try {
			
			File file = new File(path);
			BufferedReader br = null;
			String s = null;
			String values = "";
			
			/** 判断文件是否存在 */
			if(file.exists()){
				br = new BufferedReader(new FileReader(file));
				while((s = br.readLine())!=null){
					
					values = values + "('"+getUUID()+"','"+s+"')" + "," ;
					
				}
				br.close();
			}else{
				return false;
			}
			dncRosterDao.insertFile("dncphonenumber(id,phoneNum)", values.substring(0,values.length()-1));
			initForm();
		} catch (Exception e) {
			Util.error(this, e,"Exception");
			
			return false;
		}
		return true;
	}

	
	
	public boolean init(){
		
		initForm();
		
		Util.info(this, "DNCRosterMgr - init ");
		
		return true;
	}
	/**
	 * 初始化数据
	 * @return
	 */
	public boolean initForm() {
		Util.info(this, "DNCRosterMgr - initFromDB");
		
		try {
			this.dncPhoneNumList = dncRosterDao.select("dncphonenumber");
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:DNCRosterMgr function:initFromDB");
			return false;
		}
		
		return true;
	}
	/**
	 * 删除号码
	 * @param limit
	 */
	public boolean deletePhoneNum(String limit){
		
		String value = "`phoneNum`='"+limit+"'";
		
		try {
			dncRosterDao.delete("dncphonenumber", value);
			
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:DNCRosterMgr deletePhoneNum");

			return false;
		}
		
		return true;
		
	}
	
	/**
	 * 自动生成32位的UUid，对应数据库的主键id进行插入用。
	 * 
	 * @return
	 */
	public String getUUID() {

		return UUID.randomUUID().toString().replace("-", "");
	}
	
	/**
	 * 对应数据库dncphonenumber表
	 * @author DELL
	 *
	 */
    public class PhoneNumber { 
        String phoneNum;

		public PhoneNumber(String phoneNum) {
			this.phoneNum = phoneNum;
		}
		
		public PhoneNumber() {
			super();
		}



		public String getPhoneNum() {
			return phoneNum;
		}

		public void setPhoneNum(String phoneNum) {
			this.phoneNum = phoneNum;
		}
        
    }
	
}
