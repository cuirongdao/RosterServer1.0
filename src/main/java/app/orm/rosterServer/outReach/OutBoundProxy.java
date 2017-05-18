package app.orm.rosterServer.outReach;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import component.orm.IOutReach;
import component.orm.OutReachType;
import component.util.Util;
import component.orm.protocol.IResultCallBack;
import component.orm.protocol.Result;

public class OutBoundProxy implements IOutReach {
	private String outBoundUrl;
	private Map<String, IResultCallBack> callBackMap = new HashMap<String, IResultCallBack>();

	@Override
	public void onOutReachResult(String content) {
		Util.trace(this, "onOutReachResult: %s", content);
		Gson gson = new Gson();
		Result ret = new Result();
		Map<String, ?> map = (Map) gson.fromJson(content, Object.class);

		Map<String, String> resultMap = (Map<String, String>) map.get("result");
		int resultCode = Integer.valueOf((String) resultMap.get("resultCode"))
				.intValue();
		String result = (String) resultMap.get("result");
		String rosterinfo_id = (String) resultMap.get("rosterinfo_id"); // callresult表中的Id字段

		ret.setCode(resultCode);
		ret.setId(rosterinfo_id);
		ret.setReason(result);
		if (callBackMap.containsKey(rosterinfo_id)) {
			callBackMap.get(rosterinfo_id).onResultCallBack(ret);
			callBackMap.remove(rosterinfo_id);
		} else
			Util.info(this, "CallBack is not Exist: %s", rosterinfo_id);
	}

	@Override
	public boolean sendOutReach(String id, IResultCallBack callBack,
			OutReachType type, String senderAddress, String destination,
			String localNo, String uui) {
		Util.trace(this,
				"senderAddress:%s, destination:%s, localNo:%s, uui:%s",
				senderAddress, destination, localNo, uui);
		boolean flag = false;
		if (type.equals(OutReachType.call)) {
			flag = makeCall(id, destination, localNo, uui);
		}
		if (flag) {
			Util.trace(this, "put callBackMap:%s", flag);
			callBackMap.put(id, callBack);
		}
		return flag;
	}

	public boolean makeCall(String id, String caller, String called, String uui) {
		boolean flag = false;
		Map<String, String> map = new HashMap<String, String>();
		try {
			map.put("caller", caller);
			map.put("callee", called);
			map.put("uui", URLEncoder.encode(uui, "utf-8"));
			map.put("rosterinfo_id", id);
			map.put("domain", "wilcom.cn");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Gson gson = new Gson();
		String jsonStr = gson.toJson(map);
		Util.trace(this, "content： %s", jsonStr);
		try {
			flag = invokePost(jsonStr);
		} catch (IOException e) {
			Util.error(this, e, "Exception When Dial", "");
		}
		return flag;
	}

	public void invokeGet() throws IOException {
		String getURL = outBoundUrl + "?number="
				+ URLEncoder.encode("91312321321", "utf-8");
		URL getUrl = new URL(getURL);
		HttpURLConnection connection = (HttpURLConnection) getUrl
				.openConnection();
		connection.connect();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));

		String lines;
		while ((lines = reader.readLine()) != null) {
			System.out.println(lines);
		}
		reader.close();
		// 断开连接
		connection.disconnect();

	}

	public boolean invokePost(String content) throws IOException {
		Util.trace(this, "makecall: %s", content);
		// content = URLEncoder.encode(content, "utf-8");
		URL postUrl = new URL(outBoundUrl);
		// 打开连接
		HttpURLConnection connection = (HttpURLConnection) postUrl
				.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Charset", "UTF-8");
		connection.connect();
		DataOutputStream out = new DataOutputStream(
				connection.getOutputStream());

		out.writeBytes(content);

		out.flush();
		out.close(); // flush and close
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		String line;
		boolean flag = false;
		while ((line = reader.readLine()) != null) {
			Util.trace(this, "Post:%s", line);
			Gson gson = new Gson();
			Map<String, String> map = (Map) gson.fromJson(line, Object.class);
			if (map.get("code").equals("200")) {
				flag = true;
			}
		}

		reader.close();
		connection.disconnect();
		return flag;
	}

	public String getOutBoundUrl() {
		return outBoundUrl;
	}

	public void setOutBoundUrl(String outBoundUrl) {
		this.outBoundUrl = outBoundUrl;
	}

}
