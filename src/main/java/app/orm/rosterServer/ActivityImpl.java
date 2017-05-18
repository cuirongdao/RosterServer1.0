package app.orm.rosterServer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.gson.Gson;

import app.orm.rosterServer.bean.PendingContact;
import app.orm.rosterServer.mybatis.DBProxy;
import app.orm.rosterServer.outReach.OutReachFactory;
import component.orm.Activity;
import component.orm.Contact;
import component.orm.IOutReach;
import component.orm.OutReachType;
import component.orm.protocol.IResultCallBack;
import component.orm.protocol.Result;
import component.util.Util;

public class ActivityImpl extends Activity implements Job, IResultCallBack {

	private boolean bRunFlag = false;
	private DBProxy dbProxy = Util.getBean("dbProxy", DBProxy.class);
	RediaOperator redis = new RediaOperator();
	Gson gson = new Gson();

	// dialMap外呼的计时器
	// redisContent存放要外呼名单存入redis的key
	HashMap<String, Timer> dialMap;
	List<String> redisContent = new ArrayList<String>();

	// dncNumberDNC数量
	// dialTotal活动已呼出总数
	// firstStartTime最早开始时间
	// lastEndTime最晚结束时间
	int dncNumber = 0;
	int dialTotal = 0;
	int pendingOutReach = 0;
	Date firstStartTime = this.getDateFromCron("0 55 23 * * ?");
	Date lastEndTime = this.getDateFromCron("0 5 1 * * ?");

	/**
	 * 任务执行进度 1、初始化数据，根据条件 将呼叫从名单库里面提取到 2、执行外呼任务
	 */
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		Date endTime = null;
		if (this.getStatus() < 0) {
			Util.info(this, "The Activity is not runing,Name:%s Status:%s",
					this.getName(), this.getStatus());
			return;
		}
		Util.info(this, "Activity %s  Schedule start...", this.getName());

		if (arg0.getJobDetail().getJobDataMap().containsKey("EndTime")) {
			String temp = arg0.getJobDetail().getJobDataMap()
					.getString("EndTime");
			endTime = this.getDateFromCron(temp);
		}
		if (endTime != null)
			Util.info(this, "Activity %s  Schedule endTime:%s", this.getName(),
					endTime.toString());

		bRunFlag = true;
		while (bRunFlag) {
			this.setState("running");
			// Start 遍历redis中客户信息的key,发起外呼
			if (redisContent.size() > 0) {
				for (String key : redisContent) {
					PendingContact contact = redis.get(key);

					while (this.getPolicy().isSendOutReach(contact, this) == false) {

					}

					if (checkRecycle(contact)) {
						this.sendOutReach(contact);
					}
				}
			} else {
				this.extractLoadData();
			}
			// End
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Util.error(this, e, "Thread.sleep Fail...");
			}
			// Start 当前时间是否小于活动结束时间
			Date now = new Date();
			if (now.getTime() - endTime.getTime() > 0) {
				bRunFlag = false;
				this.setState("pause");
				if (now.getTime() > lastEndTime.getTime()) {
					this.setState("complete");
					insertActivityCount();
				}
				Util.info(this, "End Activity %s Schecudle", this.getName());
			}
			// End

			if (this.getPolicy().isActivityComplete(this)) {
				bRunFlag = false;
				this.setState("complete");
				insertActivityCount();
			}
		}
	}

	// 初始化
	public boolean init() {
		Util.info(this, "ActvityImpl init... ,Name:%s", this.getName());

		redis.init();

		dncNumber = getDncNumber();
		dialTotal = 0;
		this.dialMap = new HashMap<String, Timer>();

		// 判断活动是否被启用
		if (this.getStatus() < 1) {
			Util.info(this, "ActvityImpl :%s has not been Enabled...",
					this.getName());
			return true;
		}
		// start添加定时任务
		JobMgr jobMgr = Util.getBean("JobMgr", JobMgr.class);
		for (int i = 0; i < this.getPolicy().getTimeRange().size(); i++) {
			String timeRange = this.getPolicy().getTimeRange().get(i);
			Util.trace(this, "ActvityImpl timeRange...:%s", timeRange);
			String temp[] = timeRange.split("\\|");// 用|分割要写成\\|
			if (temp.length == 2 && this.getStatus() > 0) {
				// 如果定时任务已存在，先暂停，然后删除
				if (jobMgr.existsJob(this.getName() + i, "Activity")) {
					jobMgr.pauseJob(this.getName() + i, "Activity");
					jobMgr.deleteJob(this.getName() + i, "Activity");
				}
				jobMgr.NewJob(this.getName() + i, "Activity", temp[0], temp[1],
						this);

				// 当前活动的最早开始时间，最晚结束时间
				if (firstStartTime.getTime() > this.getDateFromCron(temp[0])
						.getTime()) {
					firstStartTime = this.getDateFromCron(temp[0]);
				}
				if (lastEndTime.getTime() < this.getDateFromCron(temp[1])
						.getTime()) {
					lastEndTime = this.getDateFromCron(temp[1]);
				}
			}
		}
		// end
		Util.trace(this, "firstStartTime: %s, lastEndTime: %s", firstStartTime,
				lastEndTime);
		return true;
	}

	/**
	 * 时间格式转换，cron转换成datetime
	 * 
	 * @param String
	 *            quartz格式的字符串
	 * @return Date
	 */
	@SuppressWarnings("deprecation")
	public Date getDateFromCron(final String cron) {
		String CRON_DATE_FORMAT = "ss mm HH * * ?";

		if (cron == null) {
			return null;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(CRON_DATE_FORMAT);
		Date date = null;
		try {
			date = sdf.parse(cron);
			Date now = new Date();
			// Calendar.da
			date.setYear(now.getYear());
			date.setMonth(now.getMonth());
			date.setDate(now.getDate());
		} catch (ParseException e) {
			Util.error(this, e, "getDateFromCron Fail!");
			return null;// 此处缺少异常处理,自己根据需要添加
		}
		return date;
	}

	/**
	 * 根据活动的条件从roster中抽取数据，并存入redis中
	 * 
	 * @return
	 */
	public boolean extractLoadData() {
		Util.info(this, "extractLoadData condition: %s", this.getConditions());
		List<Object> objList = null;

		// start 拼接uui
		String uui = "";
		if (this.getUui() == null) {
			Util.info(this, "extractLoadData uui: %s", this.getUui());
		} else {
			uui = "CONCAT(";
			String[] sqss = this.getUui().split(",");
			for (int k = 0; k < sqss.length; k++) {
				uui = uui + "'_',`" + sqss[k] + "`,";
			}
			uui = uui.substring(0, uui.length() - 1) + ")";
		}
		String sql = "select *," + uui + " as uui from " + this.getRosterName()
				+ " where ";
		// end

		// start 拼接查询条件
		String conditions = "";
		if (this.getConditions() != null) {
			for (int i = 0; i < this.getConditions().size(); i++) {

				String name = this.getConditions().get(i).getName();
				String type = this.getConditions().get(i).getType();
				String condition = this.getConditions().get(i).getCondition();
				if (type.equals("String")) {
					condition = condition.replace("*", "%");
					conditions = conditions + "`" + name + "` like '"
							+ condition + "' and ";
				} else if (type.equals("int")) {
					String[] arr = condition.split("and");
					for (int j = 0; j < arr.length; j++) {
						conditions = conditions + "`" + name + "` " + arr[j]
								+ " and ";
					}
				} else if (type.equals("date")) {
					String[] arr = condition.split("and");
					for (int j = 0; j < arr.length; j++) {
						conditions = conditions + "`" + name + "` " + arr[j]
								+ " and ";
					}
				}
			}
		}
		sql = sql
				+ conditions
				+ " `extractflag`=0 and contactnum not in(select phoneNum from dncphonenumber)";
		Util.trace(this, "extractLoadData - condition sql: %s", sql);
		// end

		try {
			objList = dbProxy.selectSql(sql);
		} catch (Exception e) {
			Util.error(this, e, "extractLoadData Select Fail...");
		}

		pendingOutReach = pendingOutReach + objList.size();
		// Start 遍历结果,并存入redis
		for (int i = 0; i < objList.size(); i++) {
			@SuppressWarnings("unchecked")
			HashMap<String, Object> result = (HashMap<String, Object>) objList
					.get(i);
			Util.trace(this, "objList HashMap - result: %s", result.toString());
			PendingContact contact = new PendingContact();
			contact.setId(getUUID());
			if (result.containsKey("id"))
				contact.setContactId((String) result.get("id"));
			if (result.containsKey("contactnum"))// 手机号码
				contact.setContactNum((String) result.get("contactnum"));
			if (result.containsKey("Gender")) {
				// 如果数据库中时varchar,此处报错
				contact.setGender((Integer) result.get("Gender"));
			}
			if (result.containsKey("uui"))
				contact.setUui((String) result.get("uui"));
			if (result.containsKey("FirstName")) {
				contact.setFirstName((String) result.get("FirstName"));
			}
			if (result.containsKey("LastName")) {
				contact.setLastName((String) result.get("LastName"));
			}
			if (result.containsKey("createtime")) {
				Util.trace(this, "result.get(createtime):%s",
						result.get("createtime").toString());
				Util.trace(this, ((Date) result.get("createtime")).toString());

				contact.setCreateTime((Date) result.get("createtime"));
			}
			if (result.containsKey("expire_time")) {
				Util.trace(this, "result.get(expire_time):%s",
						result.get("expire_time").toString());
				contact.setExpireTime((Date) result.get("expire_time"));
			}
			contact.setActivityId(this.getId());
			contact.setPriority(this.getPriority());
			contact.setRecycleDay(0);
			contact.setRecycleTotal(0);

			redisContent.add(this.getName() + contact.getId());
			redis.set(this.getName() + contact.getId(), gson.toJson(contact));
			// End

			try {
				dbProxy.update(this.getRosterName(), "extractflag=1", "id='"
						+ contact.getContactId() + "'");
			} catch (Exception e) {
				Util.error(this, e, "update Roster Flag Fail...");
			}
		}
		// start创建callresult表
		try {
			if (!dbProxy
					.selectWhetherTableExists(this.getName() + "CallResult")) {

				String[] col = this.getUui().split(",");

				String keyStr = "";
				for (int j = 0; j < col.length; j++) {
					keyStr = keyStr + col[j] + " varchar(50),";
				}

				String columns = "(Id varchar(50),FirstName varchar(50),LastName varchar(50),gender int,activity_id varchar(50),rosterinfo_id varchar(50),Callid varchar(50),calltimes int,Callstarttime DateTime,Callendtime DateTime,Result varchar(50),ResultCode int,contactnum varchar(50),"
						+ keyStr;
				columns = columns.substring(0, columns.lastIndexOf(",")) + ")";
				String tableName = this.getName() + "CallResult";
				if (!dbProxy.selectWhetherTableExists(tableName)) {
					dbProxy.createTable(tableName, columns);
				}
			}
		} catch (Exception e1) {
			Util.error(this, e1, "Exception When Create Table %sCallResult...",
					this.getName());
		}
		// end

		return true;
	}

	// 呼叫结果通知
	@Override
	public void onResultCallBack(Result result) {
		Util.info(this, "onResultCallBack id:%s,code:%d,reason:%s",
				result.getId(), result.getCode(), result.getReason());

		if (this.dialMap.containsKey(result.getId())) {
			// start
			PendingContact pcontact = redis
					.get(this.getName() + result.getId());
			pcontact.setReason(result.getReason());
			pcontact.setResultCode(result.getCode());

			if (result.getCode() == 0) {
				pcontact.setOnThePhone(9);

				redis.set(this.getName() + result.getId(),
						gson.toJson(pcontact));

				insertCallResult(pcontact);
			} else {
				pcontact.setOnThePhone(0);

				redis.set(this.getName() + result.getId(),
						gson.toJson(pcontact));

				if (pcontact.getRecycleDay() == this.getPolicy()
						.getRecycleDay()) {
					insertCallResult(pcontact);
				}
			}
			// end

			// start删除定时任务
			Util.trace(this, "addTimer - 删除呼叫记录: %s", result.getId());
			if (dialMap.containsKey(result.getId())) {
				dialMap.get(result.getId()).cancel();
				dialMap.remove(result.getId());
			}
			// end
		}
	}

	// 检查并发量
	public boolean checkCapcity() {
		if (this.dialMap.size() >= this.getMaxCapacity()) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Util.info(this, " checkCapcity Fail" + e.getMessage());
			}
			return false;
		}
		return true;
	}

	// 检查重复次数
	public boolean checkRecycle(PendingContact contact) {
		if (contact == null)
			return false;
		if (contact.getOnThePhone() == 1)
			return false;
		if (contact.getRecycleDay() >= this.getPolicy().getRecycleDay())
			return false;
		if (contact.getRecycleTotal() >= this.getPolicy().getRecycleTotal())
			return false;
		return true;
	}

	/**
	 * 外呼接口,发起外呼
	 * 
	 * @param Contact
	 *            外呼客户信息
	 * @return boolean
	 * */
	public boolean sendOutReach(Contact contact) {

		Util.info(this, "Activity %s sendOutReach PhoneNum:%s  ",
				this.getName(), contact.getContactNum());

		IOutReach outReach = OutReachFactory.getInstance().getOutReachProxy(
				OutReachType.call);

		dialTotal++;// 外呼次数
		addTimer(contact.getId(), contact);

		PendingContact pcontact = redis.get(this.getName() + contact.getId());

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = df.format(new Date());

		pcontact.setCallStartTime(date);
		pcontact.setRecycleDay(contact.getRecycleDay() + 1);
		pcontact.setRecycleTotal(contact.getRecycleTotal() + 1);
		pcontact.setOnThePhone(1);
		redis.set(this.getName() + contact.getId(), gson.toJson(pcontact));

		// start调用外呼接口
		String uuiStr = "";
		uuiStr = contact.getId() + "_" + contact.getActivityId()
				+ contact.getUui() + "_" + contact.getLastName();
		Util.trace(this, "uui: %s", uuiStr);
		outReach.sendOutReach(contact.getId(), this, OutReachType.call, this
				.getPolicy().getSenderAddress(), contact.getContactNum(), this
				.getLocalNo(), uuiStr);
		// end
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
	 * 添加定时任务
	 * */
	public void addTimer(String dialId, Contact contact) {
		Util.trace(this, "addTimer - 添加呼叫计时器: %s", dialId);

		final String key = this.getName() + contact.getId();
		final String dial = dialId;
		final int recycle = this.getPolicy().getRecycleDay();

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Util.trace(this, "addTimer - 更新呼叫记录: %s", dial);

				PendingContact pcontact = redis.get(key);

				pcontact.setOnThePhone(0);
				pcontact.setReason("超时");
				pcontact.setResultCode(400);
				redis.set(key, gson.toJson(pcontact));

				if (pcontact.getRecycleDay() == recycle) {
					insertCallResult(pcontact);
				}
				this.cancel();
				dialMap.remove(dial);
			}
		};
		Timer timer = new Timer();
		long delay = 60 * 1000;
		long intevalPeriod = 60 * 1000;
		timer.scheduleAtFixedRate(task, delay, intevalPeriod);

		dialMap.put(dial, timer);
	}

	/**
	 * 获取DNC数
	 * */
	public int getDncNumber() {
		int count = 0;
		String sql = "select count(*) from " + this.getRosterName()
				+ " where contactnum in(SELECT phoneNum from dncphonenumber)";
		try {
			count = dbProxy.selectCountSql(sql);
		} catch (Exception e) {
			Util.error(this, e, "getDncNumder Fail ");
		}
		return count;
	}

	/**
	 * 呼结果入库
	 * 
	 * @param PendingContact
	 *            客户信息
	 * @return null
	 * */
	public void insertCallResult(PendingContact contact) {
		Util.trace(this, "insertCallResult:%s", contact.getUui());
		String[] val = contact.getUui().split("_");
		int vallength = val.length;
		String valStr = "";
		for (int j = 1; j < vallength; j++) {
			valStr = valStr + ",'" + val[j] + "'";
		}
		Util.trace(this, "contact.getUui:%s, valStr:%s", contact.getUui(),
				valStr);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = df.format(new Date());
		String sql = "insert into "
				+ this.getName()
				+ "callresult(Id,activity_id,rosterinfo_id,gender,contactnum,firstname,lastname,calltimes,Callstarttime,Callendtime,result,resultcode,"
				+ this.getUui() + ") value('" + contact.getId() + "','"
				+ contact.getActivityId() + "','" + contact.getContactId()
				+ "'," + contact.getGender() + ",'" + contact.getContactNum()
				+ "','" + contact.getFirstName() + "','"
				+ contact.getLastName() + "','" + contact.getRecycleDay()
				+ "','" + contact.getCallStartTime() + "','" + date + "','"
				+ contact.getReason() + "','" + contact.getResultCode() + "'"
				+ valStr + ")";
		try {
			dbProxy.insert(sql);
		} catch (Exception e) {
			Util.error(this, e, "Insert into callResult Failure");
		}
		redis.delete(this.getName() + contact.getId());
		redisContent.remove(this.getName() + contact.getId());
	}

	/**
	 * 活动统计入库
	 * 
	 * @return null
	 * */
	public void insertActivityCount() {
		Util.trace(this, "insertActivityCount:%s", this.getName());

		int rosterNum = 0;
		try {
			rosterNum = dbProxy.selectCount(this.getRosterName());
		} catch (Exception e1) {
			Util.error(this, e1, "get rosterNum Fail...");
		}
		Util.trace(this, "insertActivityCount Num:%d, %d",
				this.pendingOutReach, this.redisContent.size());

		double rate = (((double) this.pendingOutReach - this.redisContent
				.size()) / this.pendingOutReach) * 100;
		int completionrate = (int) rate;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String sql = "insert into activitycount (`id`,activityname,state,starttime,endtime,rosternumber,outreachnumber,completionrate,establishrate) value('"
				+ getUUID()
				+ "','"
				+ this.getName()
				+ "','"
				+ this.getState()
				+ "','"
				+ sdf.format(this.firstStartTime)
				+ "','"
				+ sdf.format(this.lastEndTime)
				+ "','"
				+ rosterNum
				+ "','"
				+ this.dialTotal + "','" + completionrate + "',0)";
		try {
			dbProxy.insert(sql);
		} catch (Exception e) {
			Util.error(this, e, "Insert into callResult Failure");
		}
	}

}
