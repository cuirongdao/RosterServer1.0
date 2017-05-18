package app.orm.rosterServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

import app.orm.rosterServer.mybatis.DBProxy;
import component.orm.IPolicyMgr;
import component.orm.Policy;
import component.orm.QueryCondition;
import component.orm.protocol.IRequestProcessor;
import component.orm.protocol.QueryResult;
import component.orm.protocol.Request;
import component.orm.protocol.Response;
import component.orm.protocol.Result;
import component.util.Util;

/**
 * Policy实现类
 * 
 * @author sunlu
 * @version 1.0
 * */
public class PolicyMgr implements IPolicyMgr, IRequestProcessor {
	private DBProxy policyDao = Util.getBean("dbProxy", DBProxy.class);
	private List<Object> policyList = null;
	Map<String, Policy> policyMap = new HashMap<String, Policy>();

	@Override
	public Response processRequest(Request req) {
		Util.info(this, "Policy - processRequest");
		Response response = new Response();
		if (req.getMethod().equals("update")) {
			Util.info(this, "Policy - processRequest - update");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Gson gson = new Gson();
			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Policy map = gson.fromJson(json, Policy.class);

			String name = map.getName();
			Util.info(this, "Policy - processRequest - update Policy: %s", name);
			StringBuffer value = new StringBuffer();
			if (!(map.getSenderAddress() == null)) {
				value.append("senderAddress='").append(map.getSenderAddress())
						.append("',");
			}
			if (!(map.getActionTimeout() == 0)) {
				value.append("actionTimeout='").append(map.getActionTimeout())
						.append("',");
			}
			if (!(map.getRecycleDay() == 0)) {
				value.append("recycleDay='").append(map.getRecycleDay())
						.append("',");
			}
			if (!(map.getRecycleTotal() == 0)) {
				value.append("recycleTotal='").append(map.getRecycleTotal())
						.append("',");
			}
			if (!(map.getInterval() == 0)) {
				value.append("`interval`='").append(map.getInterval())
						.append("',");
			}
			if (!(map.getTimeRange() == null)) {
				value.append("timeRange='").append(map.getTimeRange())
						.append("',");
			}
			if (value.length() > 10) {
				String values = value.substring(0, value.lastIndexOf(","));
				String limit = "`name`='" + name + "'";
				try {
					policyDao.update("PolicyInfo", values, limit);
					result.setCode(0);
					result.setReason("Update PolicyInfo Success!");
				} catch (Exception e) {
					Util.error(this, e, "Update PolicyInfo Fail...");
					result.setCode(-1);
					result.setReason("Update PolicyInfo Fail...");
				}
			} else {
				result.setCode(-1);
				result.setReason("The Column You Update is Null!");
			}

			initFromDB();// 重新读取Policylist信息
			response.setResult(result);
		} else if (req.getMethod().equals("delete")) {
			Util.info(this, "Policy - processRequest - delete");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();

			Gson gson = new Gson();
			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Policy map = gson.fromJson(json, Policy.class);

			String name = map.getName();
			Util.info(this, "Policy - processRequest - delete Policy: %s", name);
			boolean flag = deletePolicy(name);
			if (flag) {
				result.setCode(0);
				result.setReason("Delete PolicyInfo success!");
			} else {
				result.setCode(-1);
				result.setReason("Delete PolicyInfo failure!");
			}

			initFromDB();// 重新读取Policylist信息
			response.setResult(result);
		} else if (req.getMethod().equals("insert")) {
			Util.info(this, "Policy - processRequest - insert");
			response.setMethod(req.getMethod());
			response.setObject(req.getObject());
			response.setType("response");
			Result result = new Result();
			Gson gson = new Gson();

			Object obj = req.getParams();
			String json = gson.toJson(obj);

			Policy policy = gson.fromJson(json, Policy.class);
			Util.info(this, "Policy - processRequest - insert Policy: %s",
					policy.getName());

			boolean flag = createPolicy(policy);

			if (flag) {
				result.setCode(0);
				result.setReason("Insert PolicyInfo Success!");
			} else {
				result.setCode(-1);
				result.setReason("Insert PolicyInfo Failure!");
			}

			initFromDB();// 重新读取Policylist信息
			response.setResult(result);
		} else if (req.getMethod().equals("queryPolicy")) {
			Util.info(this, "Policy - processRequest - queryPolicy");
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
				queryResult.setResList(policyList);
			} else {
				int beginIndex = (curPage - 1) * pageNum + 1;
				int endIndex = curPage * pageNum;
				int size = this.policyList.size();
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
					queryResult.setResList(policyList.subList(beginIndex - 1,
							size));
				} else if (endIndex <= size) {
					queryResult.setCode(0);
					queryResult.setReason("Query success!");
					queryResult.setResList(policyList.subList(beginIndex - 1,
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

	@Override
	public boolean init() {
		Util.trace(this, "PolicyMgr - init");

		initFromDB();

		return true;
	}

	@Override
	public boolean initFromDB() {
		Util.info(this, "Policy - initFromDB");
		try {
			this.policyList = policyDao.select("PolicyInfo");

			toPolicy();
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:PolisyMgr function:initFromDB");
			return false;
		}
		return true;
	}

	@Override
	public boolean createPolicy(Policy policy) {
		Util.info(this, "Policy - createPolicy:policy %s", policy.getName());
		policy.setId(getUUID());
		try {
			StringBuffer value = new StringBuffer();
			value.append(
					"PolicyInfo (tenantId,`name`,id,senderAddress,actionTimeout,recycleDay,recycleTotal,`interval`,`type`,timeRange) ")
					.append("value('").append(policy.getTenantId())
					.append("','").append(policy.getName()).append("','")
					.append(policy.getId()).append("','")
					.append(policy.getSenderAddress()).append("','")
					.append(policy.getActionTimeout()).append("','")
					.append(policy.getRecycleDay()).append("','")
					.append(policy.getRecycleTotal()).append("','")
					.append(policy.getInterval()).append("','")
					.append(policy.getPolicyType()).append("','")
					.append(policy.getTimeRange()).append("')");
			policyDao.insertJson(value.toString());
			this.policyMap.put(policy.getName(), policy);
			return true;
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:PolicyMgr function:createPolicy");
			return false;
		}
	}

	@Override
	public Policy findPolicy(String name) {
		Util.info(this, "Policy - findPolicy Policy %s", name);
		Policy policy = new Policy();
		if (policyMap.containsKey(name)) {
			policy = policyMap.get(name);
			return policy;
		}
		return policy;
	}

	@Override
	public boolean deletePolicy(String name) {
		Util.info(this, "Policy - deletePolicy Policy %s", name);
		try {
			// 删除ProlicyInfo表中的记录
			policyDao.delete("PolicyInfo", "`name`='" + name + "'");
			this.policyMap.remove(name);
		} catch (Exception e) {
			Util.error(this, e,
					"Exception. Class:PolicyMgr function:deletePolicy");

			return false;
		}
		return true;
	}

	@Override
	public boolean updatePolicy(Policy policy) {
		Util.info(this, "Policy - updatePolicy Policy %s", policy.getName());
		StringBuffer value = new StringBuffer();

		value.append("tenantId='").append(policy.getTenantId())
				.append("',senderAddress='").append(policy.getSenderAddress())
				.append("',actionTimeout=").append(policy.getActionTimeout())
				.append(",recycleDay=").append(policy.getRecycleDay())
				.append(",recycleTotal=").append(policy.getRecycleTotal())
				.append(",interval=").append(policy.getInterval())
				.append(",type=").append(policy.getPolicyType())
				.append(",timeRange='").append(policy.getTimeRange())
				.append("'");

		String values = value.toString();
		String limit = "name='" + policy.getName() + "'";
		try {
			policyDao.update("PolicyInfo", values, limit);
			this.policyMap.put(policy.getName(), policy);
		} catch (Exception e) {
			Util.error(this, e, "Exception When Update PolicyInfo");
			return false;
		}

		return true;
	}

	private void toPolicy() {
		Util.info(this, "Policy - toPolicy");
		int length = this.policyList.size();
		Map<String, Object> map = new HashMap<String, Object>();
		for (int i = 0; i < length; i++) {
			map = (Map) this.policyList.get(i);

			Policy policy = new Policy();
			if ((Integer) map.get("type") == 0) {
				policy = new PolicyAutoImpl();
			} else if ((Integer) map.get("type") == 1) {
				policy = new PolicyAutoImpl();
			}

			if (map.containsKey("recycleDay"))
				policy.setRecycleDay((Integer) map.get("recycleDay"));
			if (map.containsKey("id"))
				policy.setId((String) map.get("id"));
			if (map.containsKey("tenantId"))
				policy.setTenantId((String) map.get("tenantId"));
			if (map.containsKey("senderAddress"))
				policy.setSenderAddress((String) map.get("senderAddress"));
			if (map.containsKey("actionTimeout"))
				policy.setActionTimeout((Integer) map.get("actionTimeout"));
			if (map.containsKey("type"))
				policy.setPolicyType((Integer) map.get("type"));
			if (map.containsKey("interval"))
				policy.setInterval((Integer) map.get("interval"));
			if (map.containsKey("recycleTotal"))
				policy.setRecycleTotal((Integer) map.get("recycleTotal"));

			if (map.containsKey("timeRange")) {
				String str = (String) map.get("timeRange");
				str = str.substring(1, str.lastIndexOf("]"));

				String[] strArr = str.split(", ");

				List<String> timeRange = new ArrayList<String>();
				Collections.addAll(timeRange, strArr);

				policy.setTimeRange(timeRange);
			}
			String name = (String) map.get("name");
			policy.setName(name);

			this.policyMap.put(name, policy);
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

}
