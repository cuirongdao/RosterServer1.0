package app.orm.rosterServer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import app.orm.rosterServer.mybatis.DBProxy;
import app.orm.rosterServer.mybatis.DBUtil;
import component.orm.*;
import component.orm.protocol.IRequestProcessor;
import component.orm.protocol.QueryResult;
import component.orm.protocol.Request;
import component.orm.protocol.Response;
import component.orm.protocol.Result;
import component.util.Util;

public class ActivityMgr implements IActivityMgr, IRequestProcessor {

	Scheduler scheduler;
	HashMap<String, ActivityImpl> activityMap = new HashMap<String, ActivityImpl>();
	private List<Object> activityList = null;
	private DBProxy activityDao = Util.getBean("dbProxy", DBProxy.class);
	DBUtil dbUtil = new DBUtil();
	Gson gson = new Gson();

	@Override
	public boolean init() throws SchedulerException {
		scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();

		initFromDB();

		for (String key : activityMap.keySet()) {
			activityMap.get(key).init();
		}

		return true;
	}

	public void initFromDB() {
		Util.info(this, "Activity - initFromDB");
		try {

			this.activityList = activityDao.select("activityinfo");
			for (int i = 0; i < this.activityList.size(); i++) {
				toActivity(this.activityList.get(i));
			}
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:ActivityMgr function:initFromDB");
		}
	}

	@Override
	public boolean createActivity(Activity activity) {
		Util.info(this, "Activity - createActivity:Activity %s",
				activity.getName());
		activity.setId(getUUID());
		try {
			StringBuffer value = new StringBuffer();
			value.append(
					"activityinfo (`name`,`tenantid`,`id`,`desc`,`status`,`policyname`,`rostername`,`maxcapacity`,`priority`,`localNo`,`uui`,`condition`) value('")
					.append(activity.getName()).append("','")
					.append(activity.getTenantId()).append("','")
					.append(activity.getId()).append("','")
					.append(activity.getDesc()).append("',")
					.append(activity.getStatus()).append(",'")
					.append(activity.getPolicyName()).append("','")
					.append(activity.getRosterName()).append("',")
					.append(activity.getMaxCapacity()).append(",")
					.append(activity.getPriority()).append(",'")
					.append(activity.getLocalNo()).append("','")
					.append(activity.getUui()).append("','")
					.append(gson.toJson(activity.getConditions())).append("')");
			activityDao.insertJson(value.toString());
			return true;
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:ActivityMgr function:createActivity");
			return false;
		}
	}

	@Override
	public Activity findActivity(String name) {
		Util.info(this, "Activity - findActivity:Activity %s", name);
		Activity activity = new Activity();

		if (activityMap.containsKey(name)) {
			activity = activityMap.get(name);
			return activity;
		}

		return activity;
	}

	@Override
	public boolean deleteActivity(String name) {
		Util.info(this, "Activity - deleteActivity:Activity %s", name);
		try {
			// 删除ProlicyInfo表中的记录
			activityDao.delete("activityinfo", "`name`='" + name + "'");
			this.activityMap.remove(name);
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:ActivityMgr function:deleteActivity", "");

			return false;
		}

		return true;
	}

	@Override
	public boolean disableActivity(String name) {
		Util.info(this, "Activity - disableActivity:Activity %s", name);
		ActivityImpl actImpl = activityMap.get(name);

		JobMgr jobMgr = Util.getBean("JobMgr", JobMgr.class);
		for (int i = 0; i < actImpl.getPolicy().getTimeRange().size(); i++) {
			// | 间隔符
			String timeRange = actImpl.getPolicy().getTimeRange().get(i);
			Util.info(this, "ActvityImpl timeRange:%s", timeRange);// sunlu
			String temp[] = timeRange.split("\\|");// sunlu | -> \\|
			if (temp.length == 2) {
				if (jobMgr.existsJob(actImpl.getName() + i, "Activity")) {
					jobMgr.pauseJob(actImpl.getName() + i, "Activity");
					jobMgr.deleteJob(actImpl.getName() + i, "Activity");
				}
			}
		}

		String value = "`status`=-1";

		String limit = "`name`='" + name + "'";
		try {
			activityDao.update("activityInfo", value, limit);
			activityMap.get(name).setStatus(-1);
		} catch (Exception e) {
			Util.error(this, e, "Exception When DisableActivity");
			return false;
		}
		return true;
	}

	@Override
	public boolean enableActivity(String name) {
		Util.info(this, "Activity - enableActivity:Activity %s", name);
		String value = "`status`=1";

		String limit = "`name`='" + name + "'";
		try {
			activityDao.update("activityInfo", value, limit);
			activityMap.get(name).setStatus(1);
		} catch (Exception e) {
			Util.error(this, e, "Exception When EnableActivity");
			return false;
		}
		return true;
	}

	@Override
	public Response processRequest(Request req) {
		Util.info(this, "Activity - processRequest");
		Response response = new Response();
		if (req.getMethod().equals("update")) {
			Util.info(this, "Activity - processRequest - update");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Activity map = gson.fromJson(json, Activity.class);

			String name = map.getName();
			Util.info(this, "Activity - processRequest - update %s", name);
			if (activityMap.containsKey(name)) {
				boolean flag = updateActivity(map);
				if (flag) {
					result.setCode(0);
					result.setReason("Update ActivityInfo Success!");
				} else {
					result.setCode(-1);
					result.setReason("Exception When Update ActivityInfo!");
				}
				activityMap.get(name).init();
			} else {
				result.setCode(-1);
				result.setReason("The content You Update is not Exist! Please Create!");
			}

			initFromDB();
			response.setResult(result);
		} else if (req.getMethod().equals("delete")) {
			Util.info(this, "Activity - processRequest - delete");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Activity map = gson.fromJson(json, Activity.class);

			String name = map.getName();
			Util.info(this, "Activity - processRequest - delete %s", name);

			// 禁用活动
			disableActivity(name);

			boolean flag = deleteActivity(name);
			if (flag) {
				result.setCode(0);
				result.setReason("Delete ActivityInfo success!");
			} else {
				result.setCode(-1);
				result.setReason("Delete ActivityInfo failure!");
			}

			response.setResult(result);
		} else if (req.getMethod().equals("insert")) {
			Util.info(this, "Activity - processRequest - insert");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Activity activity = gson.fromJson(json, Activity.class);

			Util.info(this, "Activity - processRequest - insert %s",
					activity.getName());
			boolean flag = createActivity(activity);

			if (flag) {
				result.setCode(0);
				result.setReason("Insert ActivityInfo Success!");
			} else {
				result.setCode(-1);
				result.setReason("Insert ActivityInfo Failure!");
			}

			initFromDB();
			response.setResult(result);
		} else if (req.getMethod().equals("queryActivity")) {
			Util.info(this, "Activity - processRequest - queryActivity");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			QueryResult queryResult = new QueryResult();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			QueryCondition query = gson.fromJson(json, QueryCondition.class);

			int curPage = query.getCurPage();
			int pageNum = query.getPageNum();

			Util.info(this, "curPage:" + curPage);
			if (curPage <= 0) {
				queryResult.setCode(0);
				queryResult.setReason("Query success!");
				queryResult.setResList(activityList);
			} else {
				int beginIndex = (curPage - 1) * pageNum + 1;
				int endIndex = curPage * pageNum;
				int size = this.activityList.size();
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
					queryResult.setResList(activityList.subList(beginIndex - 1,
							size));
				} else if (endIndex <= size) {
					queryResult.setCode(0);
					queryResult.setReason("Query success!");
					queryResult.setResList(activityList.subList(beginIndex - 1,
							endIndex));
				}
			}
			response.setResult(queryResult);
		} else if (req.getMethod().equals("disable")) {
			Util.info(this, "Activity - processRequest - disable");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Activity map = gson.fromJson(json, Activity.class);

			String name = map.getName();
			Util.info(this, "Activity - processRequest - disable %s", name);
			boolean flag = disableActivity(name);
			if (flag) {
				result.setCode(0);
				result.setReason("disable ActivityInfo success!");
			} else {
				result.setCode(-1);
				result.setReason("disable ActivityInfo failure!");
			}

			initFromDB();
			response.setResult(result);
		} else if (req.getMethod().equals("enable")) {
			Util.info(this, "Activity - processRequest - enable");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Activity map = gson.fromJson(json, Activity.class);

			String name = map.getName();
			Util.info(this, "Activity - processRequest - enable %s", name);
			boolean flag = enableActivity(name);
			if (flag) {
				result.setCode(0);
				result.setReason("enable ActivityInfo success!");
			} else {
				result.setCode(-1);
				result.setReason("enable ActivityInfo failure!");
			}

			initFromDB();
			activityMap.get(name).init();
			response.setResult(result);
		} else if (req.getMethod().equals("queryActivityStatus")) {
			Util.info(this, "Activity - processRequest - queryActivityStatus");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			QueryResult queryResult = new QueryResult();

			int init = 0, running = 0, pause = 0, complete = 0, total = 0;

			for (String key : this.activityMap.keySet()) {
				if (this.activityMap.get(key).getState().equals("init")) {
					init++;
				} else if (this.activityMap.get(key).getState()
						.equals("running")) {
					running++;// 当天正在运行的活动数
				} else if (this.activityMap.get(key).getState().equals("pause")) {
					pause++;// 当天已开始，但已暂停的活动数
				} else if (this.activityMap.get(key).getState()
						.equals("complete")) {
					complete++;// 当天已完成的活动数
				}
				if (this.activityMap.get(key).getStatus() > 0) {
					total++;// 当天计划开始的活动数
				}
			}
			List<Object> list = new ArrayList<Object>();
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			map.put("init", init);
			map.put("running", running);
			map.put("pause", pause);
			map.put("complete", complete);
			map.put("total", total);
			list.add(map);
			queryResult.setResList(list);
			queryResult.setCode(0);
			queryResult.setReason("success");

			response.setResult(queryResult);
		} else if (req.getMethod().equals("queryActivityDetail")) {
			Util.info(this, "Activity - processRequest - queryActivityDetail");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			QueryResult queryResult = new QueryResult();

			List<Object> list = new ArrayList<Object>();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			for (String key : activityMap.keySet()) {
				if (activityMap.get(key).getStatus() > 0) {
					ActivityDetail detail = new ActivityDetail();
					detail.setName(activityMap.get(key).getName());
					detail.setType("auto");
					detail.setState(activityMap.get(key).getState());
					detail.setRosterName(activityMap.get(key).getRosterName());
					detail.setNotDailNum(activityMap.get(key).redisContent
							.size());
					detail.setEstabNum(0);
					detail.setConnectorRate(0);
					detail.setDailNum(activityMap.get(key).dialTotal);
					detail.setDncNum(activityMap.get(key).dncNumber);
					detail.setFirstStartTime(sdf.format(activityMap.get(key).firstStartTime));
					detail.setLastEndTime(sdf.format(activityMap.get(key).lastEndTime));
					list.add(detail);
				}
			}

			queryResult.setResList(list);
			queryResult.setCode(0);
			queryResult.setReason("success");

			response.setResult(queryResult);
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

	@Override
	public boolean updateActivity(Activity activity) {
		Util.trace(this, "ActivityMgr - updateActivity Activity %s",
				activity.getName());
		StringBuffer value = new StringBuffer();

		value.append("`tenantId`='").append(activity.getTenantId())
				.append("',`desc`='").append(activity.getDesc())
				.append("',`status`=").append(activity.getStatus())
				.append(",`policyname`='").append(activity.getPolicyName())
				.append("',`rostername`='").append(activity.getRosterName())
				.append("',`maxCapacity`=").append(activity.getMaxCapacity())
				.append(",`priority`=").append(activity.getPriority())
				.append(",`localNo`='").append(activity.getLocalNo())
				.append("',`uui`='").append(activity.getUui())
				.append("',`condition`='")
				.append(gson.toJson(activity.getConditions())).append("'");

		// String values = value.substring(0, value.lastIndexOf(","));
		String values = value.toString();
		String limit = "name='" + activity.getName() + "'";
		try {
			activityDao.update("activityinfo", values, limit);
		} catch (Exception e) {
			Util.error(this, e, "Exception When Update activityinfo", "");
			return false;
		}

		return true;
	}

	public void toActivity(Object obj) {
		Util.trace(this, "ActivityMgr - toActivity");

		PolicyMgr policyMgr = Util.getBean("PolicyMgr", PolicyMgr.class);
		RosterMgr rosterMgr = Util.getBean("RosterMgr", RosterMgr.class);
		HashMap<String, Object> map = new HashMap<String, Object>();

		ActivityImpl activity = new ActivityImpl();

		map = (HashMap<String, Object>) obj;

		String name = (String) map.get("name");

		if (map.containsKey("desc"))
			activity.setDesc((String) map.get("desc"));
		if (map.containsKey("id"))
			activity.setId((String) map.get("id"));
		if (map.containsKey("tenantId"))
			activity.setTenantId((String) map.get("tenantId"));
		if (map.containsKey("localNo"))
			activity.setLocalNo((String) map.get("localNo"));
		if (map.containsKey("uui"))
			activity.setUui((String) map.get("uui"));
		if (map.containsKey("status"))
			activity.setStatus((Integer) map.get("status"));
		if (map.containsKey("maxcapacity"))
			activity.setMaxCapacity((Integer) map.get("maxcapacity"));
		if (map.containsKey("priority"))
			activity.setPriority((Integer) map.get("priority"));

		if (map.containsKey("condition")) {
			List<DbColumn> list = gson.fromJson((String) map.get("columns"),
					new TypeToken<List<DbColumn>>() {
					}.getType());
			activity.setConditions(list);
		}

		if (map.containsKey("rostername")) {
			String rosterName = (String) map.get("rostername");
			if (rosterMgr.rosterMap.containsKey(rosterName)) {
				activity.setRoster(rosterMgr.rosterMap.get(rosterName));
			}
			activity.setRosterName(rosterName);
		}

		if (map.containsKey("policyname")) {
			String policyName = (String) map.get("policyname");
			if (policyMgr.policyMap.containsKey(policyName)) {
				activity.setPolicy(policyMgr.policyMap.get(policyName));
			}
			activity.setPolicyName(policyName);
		}
		activity.setState("init");
		activity.setName(name);

		this.activityMap.put(name, activity);
	}

	/*
	 * 自动生成32位的UUid，对应数据库的主键id进行插入用。
	 * 
	 * @return
	 */
	public String getUUID() {

		return UUID.randomUUID().toString().replace("-", "");
	}

	public void clear() {
		Util.trace(this, "ActivityMgr - clear");
		for (String key : activityMap.keySet()) {
			deletePendingOutReach(activityMap.get(key));
		}

		updatePendingOutReach();
	}

	public void deletePendingOutReach(ActivityImpl activity) {
		Util.trace(this, "ActivityMgr - deletePendingOutReach:Activity %s",
				activity.getName());
		String sql = "(expiretime<now() or recycletotal<"
				+ activity.getPolicy().getRecycleTotal() + ") and activityid='"
				+ activity.getId() + "'";

		try {
			activityDao.delete("pendingoutreachlist", sql);
		} catch (Exception e) {
			Util.error(this, e, "deletePendingOutReach failure");
		}
	}

	public void updatePendingOutReach() {
		Util.trace(this, "ActivityMgr - updatePendingOutReach");
		try {
			activityDao
					.update("pendingoutreachlist",
							"recycleday=0",
							"DATE_FORMAT(lasttime,\"%Y%m%d\")<DATE_FORMAT(CURDATE(),\"%Y%m%d\") and recycleday>0");
		} catch (Exception e) {
			Util.error(this, e, "updatePendingOutReach failure");
		}
	}
}
