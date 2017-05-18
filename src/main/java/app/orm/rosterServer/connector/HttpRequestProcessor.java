package app.orm.rosterServer.connector;

import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import app.orm.rosterServer.ActivityMgr;
import app.orm.rosterServer.DNCRosterMgr;
import app.orm.rosterServer.PolicyMgr;
import app.orm.rosterServer.RosterMgr;
import app.orm.rosterServer.outReach.OutReachFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import component.orm.OutReachType;
import component.orm.protocol.IRequestProcessor;
import component.orm.protocol.Request;
import component.orm.protocol.Response;
import component.util.Util;

/**
 * htp请求处理
 * 
 * @author bizheng
 *
 */
public class HttpRequestProcessor implements IRequestProcessor {

	@Override
	public Response processRequest(Request request) {

		if (request.getObject().equals("Activity")) {
			ActivityMgr activityMgr = Util.getBean("ActivityMgr",
					ActivityMgr.class);
			return activityMgr.processRequest(request);
			// if(request.getParams() instanceof Activity)
		} else if (request.getObject().equals("Policy")) {
			PolicyMgr policyMgr = Util.getBean("PolicyMgr", PolicyMgr.class);
			return policyMgr.processRequest(request);
		} else if (request.getObject().equals("Roster")) {
			RosterMgr rosterMgr = Util.getBean("RosterMgr", RosterMgr.class);
			return rosterMgr.processRequest(request);
		} else if (request.getObject().equals("DNCRoster")) {
			DNCRosterMgr dncRosterMgr = Util.getBean("DNCRosterMgr",
					DNCRosterMgr.class);
			return dncRosterMgr.processRequest(request);
		} else if (request.getObject().equals("QueryCondition")) {

			if ("queryRoster".equals(request.getMethod())) {
				RosterMgr rosterMgr = Util
						.getBean("RosterMgr", RosterMgr.class);
				return rosterMgr.processRequest(request);
			} else if ("queryPolicy".equals(request.getMethod())) {
				// 这里有原来的getObject改为getMethod
				PolicyMgr policyMgr = Util
						.getBean("PolicyMgr", PolicyMgr.class);
				return policyMgr.processRequest(request);
			} else if ("queryActivity".equals(request.getMethod())) {
				// 查询活动的处理
				ActivityMgr activityMgr = Util.getBean("ActivityMgr",
						ActivityMgr.class);
				return activityMgr.processRequest(request);
			} else if ("queryActivityStatus".equals(request.getMethod())) {
				// 监控活动的处理
				ActivityMgr activityMgr = Util.getBean("ActivityMgr",
						ActivityMgr.class);
				return activityMgr.processRequest(request);
			} else if ("queryActivityDetail".equals(request.getMethod())) {
				// 监控活动的处理
				ActivityMgr activityMgr = Util.getBean("ActivityMgr",
						ActivityMgr.class);
				return activityMgr.processRequest(request);
			} else if ("queryDNCRoster".equals(request.getMethod())) {
				// queryDNCRoster
				DNCRosterMgr dncRosterMgr = Util.getBean("DNCRosterMgr",
						DNCRosterMgr.class);
				return dncRosterMgr.processRequest(request);
			}
		}

		return null;
	}

	/**
	 * 将http内容转换成request对象，然后处理返回
	 * 
	 * @param msg
	 * @return
	 */
	public String processHttpRequest(String uri, String msg) {
		// String strBuf = msg.toString(io.netty.util.CharsetUtil.UTF_8);
		if (msg.length() < 1)
			return "{}";
		Util.info(this, "Receive HttpRequest uri:%s,msg(front 50 size):%s ...",
				uri, msg.substring(0, msg.length() > 50 ? 50 : msg.length()));

		if (uri.equals("/RosterUI")) {
			return processUIRequest(msg);
		} else if (uri.equals("/RosterUI/UpLoad")) {
			HttpPostRequestDecoder decoder;

			Util.info(this, "Receive HttpRequest uri:%s,msg:%s ...", uri, msg);
		} else if (uri.equals("/OutReach")) {
			OutReachFactory.getInstance().getOutReachProxy(OutReachType.call)
					.onOutReachResult(msg);
			return "{\"code\":\"1\"}";
		}
		return "";

	}

	public String processUIRequest(String msg) {
		Gson gson = new Gson();
		Request request;
		// System.out.println(msg);
		Util.info(this, "process UIHttpRequest msg:%s", msg);
		try {
			JsonObject json = new JsonObject();
			JsonParser parser = new JsonParser();
			JsonObject jsonObject = parser.parse(msg).getAsJsonObject();
			JsonElement element = jsonObject.get("params");
			String params = element.toString();
			String object = jsonObject.get("object").getAsString();
			request = new Request();
			request.setMethod(jsonObject.get("method").getAsString());
			request.setObject(object);
			request.setType(jsonObject.get("type").getAsString());

			if ("DNCRoster".equals(object)) {
				DNCRosterMgr dncRosterMgr = Util.getBean("DNCRosterMgr",
						DNCRosterMgr.class);
				request.setParams(gson.fromJson(params,
						dncRosterMgr.new PhoneNumber().getClass()));
			} else {
				request.setParams(gson.fromJson(params,
						Class.forName("component.orm." + object)));
			}

		} catch (Throwable e) {
			Util.error(this, e, "Process http UIrequest Fail! ");
			return "{\"code\":-1,\"reason\":\"Parse Fail!\"}";
		}
		Response response = processRequest(request);
		Util.trace(this, "Response:%s", gson.toJson(response));
		return gson.toJson(response);
	}

}
