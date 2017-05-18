package app.orm.rosterServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import app.orm.rosterServer.mybatis.DBUtil;
import app.orm.rosterServer.mybatis.DBProxy;
import component.orm.DbColumn;
import component.orm.IRosterMgr;
import component.orm.Roster;
import component.orm.protocol.IRequestProcessor;
import component.orm.QueryCondition;
import component.orm.protocol.QueryResult;
import component.orm.protocol.Request;
import component.orm.protocol.Response;
import component.orm.protocol.Result;
import component.util.Util;

/**
 * Roster实现类
 * 
 * @author sunlu
 * @version 2.0
 */
public class RosterMgr implements IRosterMgr, IRequestProcessor {
	DBProxy rosterDao = Util.getBean("dbProxy", DBProxy.class);
	private DBUtil dbUtil = new DBUtil();
	private List<Object> rosterList = new ArrayList<Object>();
	Map<String, Roster> rosterMap = new HashMap<String, Roster>();

	private Gson gson = new Gson();

	/**
	 * 查询数据库中是否已经存在要创建的表，如果不存在就创建，否则不创建
	 * 
	 * @param Roster
	 *            Roster:表信息
	 * @return boolean
	 */
	public boolean createRoster(Roster roster) {
		Util.info(this, "Roster - createRoster: Roster %s", roster.getName());
		String name = roster.getName();
		// 如果要创建的roster存在，返回false
		if (rosterMap.containsKey(name)) {
			Util.info(this, "Roster - createRoster:Rostre %s is Exist, return",
					name);
			return false;
		}
		boolean flag = false;
		roster.setId(getUUID());
		// start 表是否已存在
		try {
			flag = rosterDao.selectWhetherTableExists(name);
		} catch (Exception e) {
			Util.error(this, e, "Exception.RosterMgr selectWhetherTableExists");
			return false;
		}
		// end
		if (!flag) {
			// start建表
			String column = dbUtil.jointStr(roster.getColumns());
			try {
				rosterDao.createTable(name, column);
			} catch (Exception e) {
				Util.error(this, e, "Exception. RosterMgr createTable");
				return false;
			}
			// end
			// start插入到list表中
			StringBuffer value = new StringBuffer();
			value.append(
					"rosterlist (`name`,`id`,`tenantId`,`desc`,`columns`,`createTime`,`lastModifyTime`,`importPath`,`importmode`,`importTime`,`lastImportTime`,`lastImportStatus`,`expireDays`) ")
					.append("value('").append(name).append("','")
					.append(roster.getId()).append("','")
					.append(roster.getTenantId()).append("','")
					.append(roster.getDesc()).append("','")
					.append(gson.toJson(roster.getColumns())).append("','")
					.append(roster.getCreateTime()).append("','")
					.append(roster.getLastModifyTime()).append("','")
					.append(roster.getImportPath()).append("','")
					.append(roster.getImportmode()).append("','")
					.append(roster.getImportTime()).append("','")
					.append(roster.getLastImportTime()).append("','")
					.append(roster.getLastImportStatus()).append("','")
					.append(roster.getExpireDays()).append("')");
			try {
				rosterDao.insertJson(value.toString());
			} catch (Exception e) {
				try {
					// 插入失败则删除表
					rosterDao.dropTable(name);
				} catch (Exception e1) {
					Util.error(this, e, "Exception. RosterMgr dropTable");
					return false;
				}
				Util.error(this, e, "Exception. RosterMgr insertJson");
				return false;
			}
		}
		addJob(roster);// 添加任务
		return true;

	}

	public boolean createDNCRoster(Roster roster) {

		try {
			Util.info(this, "createDNCRoster");
			rosterDao.insertFile("dncroster(id,name,createTime)",
					"('" + getUUID() + "','" + roster.getName() + "','"
							+ roster.getCreateTime() + "')");

		} catch (Exception e) {
			Util.error(this, e, "Exception");
			return false;
		}
		return true;
	}

	/**
	 * 根据表名删除表
	 * 
	 * @param String
	 *            name:表名
	 * @return boolean
	 */
	public boolean deleteRoster(String name) {
		Util.info(this, "Roster - deleteRoster: Roster %s", name);
		name = "" + name;
		// 如果要导入的roster不存在，返回false
		if (!rosterMap.containsKey(name)) {
			Util.info(this, "Roster - deleteRoster %s is not Exist, return",
					name);
			return false;
		}
		try {
			// 删除表
			rosterDao.dropTable(name);
			// 删除rosterlist表中的记录
			rosterDao.delete("rosterList", "`name`='" + name + "'");
			this.rosterMap.remove(name);
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:RosterMgr function:deleteRoster");

			return false;
		}

		return true;
	}

	/**
	 * 根据表名查询创建rosterinfo表记录
	 * 
	 * @param String
	 *            name：联系人姓名
	 * @return AbstractRoster
	 */
	public Roster findRoster(String name) {
		Util.info(this, "Roster - findRoster: Roster %s", name);
		name = "" + name;

		if (rosterMap.containsKey(name)) {
			return rosterMap.get(name);
		} else {
			Util.info(this, "Roster - findRoster %s is not Exist, return", name);
			return null;
		}
	}

	/**
	 * 初始化数据
	 * 
	 * @param null
	 * @return boolean
	 */
	public boolean initRosterFromDB() {
		Util.info(this, "Roster - initRosterFromDB");
		try {

			this.rosterList = rosterDao.select("rosterList");
			Util.info(this, "this.rosterList.size: " + this.rosterList.size());

			toRoster();
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:RosterMgr function:initRosterFromDB");

			return false;
		}
		return true;
	}

	/**
	 * 根据路径读取本地文件并导入表中
	 * 
	 * @param String
	 *            name 表名
	 * @param String
	 *            filePath 本地文件路径
	 * @return boolean
	 */
	public boolean loadFromFile(String name, String filePath) {
		Util.info(this, "Roster - loadFromFile: Roster %s", name);
		name = "" + name;

		// 如果要导入的roster不存在，返回false
		if (!rosterMap.containsKey(name)) {
			Util.info(this,
					"Roster - loadFromFile: Roster %s Load is not Exist", name);
			return false;
		}

		// 获取过期时间
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = rosterMap.get(name).getCreateTime();
		int expireDay = rosterMap.get(name).getExpireDays();
		String expireDate = "";
		try {
			Date date = df.parse(dateStr);

			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(Calendar.DATE, expireDay);// 把日期往后增加一天.整数往后推,负数往前移动
			date = calendar.getTime(); // 这个时间就是日期往后推一天的结果

			expireDate = df.format(date);
		} catch (ParseException e) {
			Util.error(this, e, "decode timemat failure");
		}

		File file = new File(filePath);
		BufferedReader reader = null;
		String tempString = null;
		String insertValues = null;
		String[] arr = null;
		int[] arrColumnNum = null;
		int line = 0;
		String columns = new String();

		try {
			Util.trace(this, "Load File Content Begin...", "");
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "utf-8"));
			StringBuffer value = new StringBuffer();
			while ((tempString = reader.readLine()) != null) {
				arr = tempString.split(",");
				boolean flag = true;
				line++;
				if (line % 5000 != 0) {
					if (line != 1) {
						if (flag) {
							value.append(dbUtil.jointSql(arr, expireDate))
									.append(", ");
						} else {
							value.append(
									dbUtil.jointSql(arr, arrColumnNum,
											expireDate)).append(", ");
						}
					} else {
						String[] arrColumn = rosterDao.selectTableColumn(name);
						arrColumnNum = new int[arrColumn.length - 3];// -3:默认添加三个字段

						for (int i = 0; i < arrColumn.length; i++) {
							for (int j = 0; j < arr.length; j++) {
								if (arrColumn[i].equals(arr[j])) {
									arrColumnNum[i] = j;
									break;
								}
							}

							if (i != arrColumn.length - 1) {
								columns = columns + arrColumn[i] + ",";
							} else {
								columns = columns + arrColumn[i] + ")";
							}
						}
						for (int k = 0; k < arrColumnNum.length; k++) {
							if (arrColumnNum[k] != k) {
								flag = false;
							}
						}
					}
				} else {
					insertValues = value.toString();
					insertValues = insertValues.substring(0,
							insertValues.lastIndexOf(","));

					columns = columns + "id,expire_time,extractflag)";
					rosterDao.insertFile(name + "(" + columns, insertValues);
					value = new StringBuffer();

					if (flag) {
						value.append(
								dbUtil.jointSql(arr, arrColumnNum, expireDate))
								.append(", ");
					} else {
						value.append(dbUtil.jointSql(arr, expireDate)).append(
								", ");
					}
				}
			}
			Util.trace(this, "Total Number：" + line, "");
			if (value.length() > 0) {
				insertValues = value.toString();
				insertValues = insertValues.substring(0,
						insertValues.lastIndexOf(","));
				rosterDao.insertFile(name + "(" + columns, insertValues);
			}

			updateRosterList(name, "success");

			initRosterFromDB();// 重新读取rosterlist信息
			// 关闭文件流
			reader.close();
			return true;

		} catch (FileNotFoundException e) {
			Util.error(this, e,
					"File Not Found Exception. Class:RosterMgr function:loadFromFile");

			updateRosterList(name, "failure");
			return false;
		} catch (IOException e) {
			Util.error(this, e,
					"IO Exception. Class:RosterMgr function:loadFromFile");

			updateRosterList(name, "failure");
			return false;
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:RosterMgr function:loadFromFile");

			updateRosterList(name, "failure");
			return false;
		}
	}

	/**
	 * 插入特定的联系人
	 * 
	 * @param String
	 *            name 表名
	 * @param String
	 *            content 要插入内容的Json字串
	 * @return boolean
	 */
	public boolean loadFromJson(String name, String content) {
		Util.info(this, "Roster - loadFromJson: Roster %s", name);
		name = "" + name;
		// 如果要导入的roster不存在，返回false
		if (!rosterMap.containsKey(name)) {
			Util.info(this,
					"Roster - loadFromJson: Roster %s Load is not Exist", name);
			return false;
		}
		Object obj = new Object();
		JsonArray jsonArray = dbUtil.parserJsonArray(content);
		Iterator<?> it = jsonArray.iterator();
		String strColumn = null;
		while (it.hasNext()) {// 遍历JsonArray对象

			obj = gson.fromJson((JsonObject) it.next(), Object.class);

			Map<String, String> map = (Map<String, String>) obj;

			strColumn = dbUtil.jointSql(map);
			try {
				rosterDao.insertJson(name + strColumn);
			} catch (Exception e) {
				Util.error(this, e,
						"Exception. Class:IRosterMgrImpl function:loadFromJson");

				updateRosterList(name, "failure");
				return false;
			}
		}
		updateRosterList(name, "success");
		return true;
	}

	public Response processHandMake(Map<String, String> map) {
		Util.info(this, "Roster - processHandMake: %s", map.toString());
		Roster roster = new Roster();

		if (map.containsKey("type")) { // 如果是再次导入
			// 名单名称
			if (map.containsKey("nameCall")) {
				roster.setName(map.get("nameCall"));
			}
			// 上传的文件是再次导入的名单
			if (map.get("type").equals("reRoster")) {
				roster = rosterMap.get(roster.getName());
				loadFromFile(roster.getName(), roster.getImportPath());
				return null;
			} else if (map.get("type").equals("roster")) { // 第一次上传名单
				if (map.containsKey("uploadType")) {
					roster.setImportmode("hm");
				}
				if (map.containsKey("filePath")) {
					String filePath = map.get("filePath").replaceAll("\\\\",
							"/");
					roster.setImportPath(filePath);
				}
				if (map.containsKey("expireDays")) {
					roster.setExpireDays(Integer.parseInt(map.get("expireDays")));

					SimpleDateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					String createTime = df.format(new Date());

					roster.setCreateTime(createTime);
				}
				if (map.containsKey("columns")) {
					List<DbColumn> list = new ArrayList<DbColumn>();
					String[] columns = (map.get("columns")).split(",");
					int length = columns.length;
					for (int i = 0; i < length; i++) {
						DbColumn col = new DbColumn();
						String[] column = columns[i].split("-");
						col.setName(column[0]);
						col.setType(column[1]);
						list.add(col);
					}
					roster.setColumns(list);
				}

				Boolean flag = createRoster(roster);
				initRosterFromDB();

				if (flag) {
					loadFromFile(roster.getName(), roster.getImportPath());
				}
			} else { // 上传不呼叫名单

				Util.info(this, "DNCRoster: %s", roster.getName());

				if (map.containsKey("filePath")) {
					String filePath = map.get("filePath").replaceAll("\\\\",
							"/");
					roster.setImportPath(filePath);
				}

				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				String createTime = df.format(new Date());
				roster.setCreateTime(createTime);

				boolean flag = createDNCRoster(roster);

				if (flag) {
					Util.info(this, "DNCRoster - insertPhoneNumber: %s",
							roster.getImportPath());
					DNCRosterMgr dncRosterMgr = Util.getBean("DNCRosterMgr",
							DNCRosterMgr.class);

					dncRosterMgr.loadDNCFileContentIntoDB(roster
							.getImportPath());
				}

			}
		}

		return null;
	}

	@Override
	public Response processRequest(Request req) {
		Util.info(this, "Roster - processRequest");
		Response response = new Response();
		if (req.getMethod().equals("update")) {
			Util.info(this, "Roster - processRequest - update");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			String rosterJson = gson.toJson(req.getParams());

			Roster obj = null; // 此处直接转Roster对象会报错
			obj = gson.fromJson(rosterJson, Roster.class);
			String tableName = obj.getName();
			Util.info(this, "Roster - processRequest - update: Roster: %s",
					tableName);

			if (rosterMap.containsKey(tableName)) {
				StringBuffer value = new StringBuffer();
				if (!(obj.getImportPath() == null)) {
					value.append("importPath='").append(obj.getImportPath())
							.append("',");
				}
				if (!(obj.getDesc() == null)) {
					value.append("`desc`='").append(obj.getDesc()).append("',");
				}
				if (!(obj.getImportmode() == null)) {
					value.append("importmode='").append(obj.getImportmode())
							.append("',");
				}
				if (!(obj.getImportTime() == null)) {
					value.append("importTime='").append(obj.getImportTime())
							.append("',");
				}
				if (!(obj.getExpireDays() == 0)) {
					value.append("expireDays=").append(obj.getExpireDays())
							.append(",");
				}
				if (value.length() > 10) {
					value = value.append("lastModifyTime='")
							.append(getNowTime()).append("'");
					String values = value.toString();
					Util.info(this, values);
					String limit = "`name`='" + tableName + "'";
					try {
						rosterDao.update("rosterList", values, limit);
						result.setCode(0);
						result.setReason("Update Roster Success!");
					} catch (Exception e) {
						Util.error(this, e, "Exception When Update Roster");
						result.setCode(-1);
						result.setReason("Exception When Update Roster!");
					}
				} else {
					// 如果要更新的roster不存在，返回-1
					result.setCode(-1);
					result.setReason("The Roster You Update is Null!");
				}
				initRosterFromDB();// 重新读取rosterlist信息

				Roster roster = rosterMap.get(tableName);
				addJob(roster);
			} else {
				result.setCode(-1);
				result.setReason("The Roster You Update is not Exist!");
			}

			response.setResult(result);
		} else if (req.getMethod().equals("delete")) {
			Util.trace(this, "processRequest - delete");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Roster obj = null;
			obj = (Roster) req.getParams();
			String name = obj.getName();
			Util.info(this, "Roster - processRequest - delete: Roster: %s",
					name);

			// 如果要删除的roster不存在，返回-1
			if (!rosterMap.containsKey(name)) {
				result.setCode(-1);
				result.setReason("The Table " + name
						+ " You Delete is not Exist!");
				Util.info(this, "The Table %s You Delete is not Exist!", name);
			} else {
				boolean flag = deleteRoster(name);
				if (flag) {
					JobMgr jobMgr = Util.getBean("JobMgr", JobMgr.class);
					if (jobMgr.existsJob(name, "Roster")) {
						jobMgr.pauseJob(name, "Roster");
						jobMgr.deleteJob(name, "Roster");
					}

					result.setCode(0);
					result.setReason("Delete Table success!");
				} else {
					result.setCode(-1);
					result.setReason("Delete Table failure!");
				}
				initRosterFromDB();// 重新读取rosterlist信息
			}
			response.setResult(result);
		} else if (req.getMethod().equals("insert")) {
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Roster roster = gson.fromJson(json, Roster.class);

			Util.info(this, "Roster - processRequest - insert: Roster: %s",
					roster.getName());

			// 如果roster名不存在就创建，否则返回-1
			if (!rosterMap.containsKey(roster.getName())) {
				boolean flag = createRoster(roster);

				if (flag) {
					initRosterFromDB();// 重新读取rosterlist信息
					result.setCode(0);
					result.setReason("Create Table Success!");
				} else {
					result.setCode(-1);
					result.setReason("Create Table Failure!");
				}
			} else {
				result.setCode(-1);
				result.setReason("The Table You Create is Already Exist!");
			}

			response.setResult(result);
		} else if (req.getMethod().equals("queryRoster")) {
			Util.info(this, "Roster - processRequest - queryRoster");
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
				queryResult.setResList(rosterList);
			} else {
				int beginIndex = (curPage - 1) * pageNum + 1;
				int endIndex = curPage * pageNum;
				int size = rosterList.size();
				int pageCount = 0;
				if (size % pageNum == 0) {
					pageCount = size / pageNum;
				} else {
					pageCount = size / pageNum + 1;
				}

				queryResult.setCurPage(curPage);
				queryResult.setCount(size);
				queryResult.setPageCount(pageCount);
				// 如果起始位大于总数，说明超出长度，返回-1；
				// 如果起始位小于等于总数，结束位大于总数，说明查询的是最后一页，返回起始位到最后一位
				// 如果结束位小于等于总数，说明查询的是总数的中间一部分，返回起始位到结束位
				if (beginIndex > size) {
					queryResult.setCode(-1);
					queryResult.setReason("Beyond Max Size!");
				} else if (beginIndex <= size && endIndex > size) {
					queryResult.setCode(0);
					queryResult.setReason("Query success!");
					queryResult.setResList(rosterList.subList(beginIndex - 1,
							size));
				} else if (endIndex <= size) {
					queryResult.setCode(0);
					queryResult.setReason("Query success!");
					queryResult.setResList(rosterList.subList(beginIndex - 1,
							endIndex));
				}
			}
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

	private String getNowTime() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = df.format(new Date());
		return date;

	}

	private void updateRosterList(String name, String state) {
		Util.info(this, "Roster - updateRosterList");
		String limit = "`name`='" + name + "'";
		String updateValue = "lastImportTime='" + getNowTime()
				+ "', lastImportStatus='" + state + "'";
		try {
			rosterDao.update("rosterList", updateValue, limit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean init() {
		Util.trace(this, "Roster - Init!!!");

		initRosterFromDB();

		for (String key : rosterMap.keySet()) {
			addJob(rosterMap.get(key));
		}

		return true;
	}

	public void toRoster() {
		Util.trace(this, "RosterMgr - toRoster!!!");

		int length = this.rosterList.size();
		HashMap<String, Object> map = new HashMap<String, Object>();
		for (int i = 0; i < length; i++) {
			Roster roster = new Roster();
			map = (HashMap) this.rosterList.get(i);

			String name = (String) map.get("name");
			roster.setName(name);

			if (map.containsKey("desc"))
				roster.setDesc((String) map.get("desc"));
			if (map.containsKey("id"))
				roster.setId((String) map.get("id"));
			if (map.containsKey("tenantId"))
				roster.setTenantId((String) map.get("tenantId"));
			if (map.containsKey("createtime"))
				roster.setCreateTime((String) map.get("createtime"));
			if (map.containsKey("expireDays"))
				roster.setExpireDays((Integer) map.get("expireDays"));
			if (map.containsKey("lastModifyTime"))
				roster.setLastModifyTime((String) map.get("lastModifyTime"));
			if (map.containsKey("importPath"))
				roster.setImportPath((String) map.get("importPath"));
			if (map.containsKey("importmode"))
				roster.setImportmode((String) map.get("importmode"));
			if (map.containsKey("importTime"))
				roster.setImportTime((String) map.get("importTime"));
			if (map.containsKey("lastImportTime"))
				roster.setLastImportTime((String) map.get("lastImportTime"));
			if (map.containsKey("lastImportStatus"))
				roster.setLastImportStatus((String) map.get("lastImportStatus"));

			if (map.containsKey("columns")) {
				List<DbColumn> list = gson.fromJson(
						(String) map.get("columns"),
						new TypeToken<List<DbColumn>>() {
						}.getType());
				roster.setColumns(list);
			}

			this.rosterMap.put(name, roster);
		}
	}

	/**
	 * 自动生成32位的UUid，对应数据库的主键id进行插入用。
	 * 
	 * @return
	 */
	public String getUUID() {

		return UUID.randomUUID().toString().replace("-", "");
	}

	public void addJob(Roster roster) {

		JobMgr jobMgr = Util.getBean("JobMgr", JobMgr.class);

		if (roster.getImportmode().equals("auto")) {
			RosterImpl rosterImpl = new RosterImpl();
			Util.trace(this, "Add New Job: %s,ImportPath(): %s,ImportTime: %s",
					roster.getName(), roster.getImportPath(),
					roster.getImportTime());
			rosterImpl.setname(roster.getName());
			rosterImpl.setFilePath(roster.getImportPath());
			if (jobMgr.existsJob(roster.getName(), "Roster")) {
				jobMgr.pauseJob(roster.getName(), "Roster");
				jobMgr.deleteJob(roster.getName(), "Roster");
			}
			jobMgr.NewJob(roster.getName(), "Roster", roster.getImportTime(),
					"", rosterImpl);
		}
	}

	public boolean checkNumber(ActivityImpl activityImpl) {
		return false;
	}
}
