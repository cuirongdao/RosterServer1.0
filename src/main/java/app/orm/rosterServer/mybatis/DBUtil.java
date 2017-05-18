package app.orm.rosterServer.mybatis;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import component.orm.DbColumn;

/**
 * json数据操作类
 * 
 * @author sunlu
 * @version 1.0
 * */
public class DBUtil {

	private Gson gson = new Gson();

	/**
	 * 解析json字串
	 * 
	 * @param String
	 *            要解析的字串
	 * @return JsonArray 解析后的json数组
	 * */
	public JsonArray parserJsonArray(String strJson) {
		// 创建一个JsonParser
		JsonParser parser = new JsonParser();
		// 通过JsonParser对象可以把json格式的字符串解析成一个JsonElement对象
		JsonElement el = parser.parse(strJson);

		// 把JsonElement对象转换成JsonArray
		JsonArray jsonArray = null;
		if (el.isJsonArray()) {
			jsonArray = el.getAsJsonArray();
		}
		return jsonArray;
	}

	/**
	 * 创建表时，组合表名json数组中的数据
	 * 
	 * @param JsonArray
	 * @return String
	 * */
	public String jointStr(List<DbColumn> jsonArray) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append("(");
		for (DbColumn column : jsonArray) {
			strBuf.append(column.getName()).append(" ")
					.append(column.getType()).append(",");
		}
		strBuf.append("id varchar(50),expire_time Datetime, extractflag int)");
		String str = strBuf.toString();

		return str;
	}

	/**
	 * 拼接插入数据的SQL语句(文件中字段顺序和表中不一致时)
	 * 
	 * @param String
	 *            []
	 * @param int[]
	 * @return String
	 * */
	public String jointSql(String[] arr, int[] arrNum, String expireDate) {
		int length = arrNum.length;
		StringBuffer value = new StringBuffer();
		value.append("('");
		for (int i = 0; i < length - 1; i++) {
			value.append(arr[arrNum[i]] + "','");
		}

		value.append(arr[arrNum[length - 1]] + "','" + getUUID() + "','"
				+ expireDate + "','0')");
		String insertValue = value.toString();
		return insertValue;
	}

	/**
	 * 拼接插入数据的SQL语句(文件中字段顺序和表中一致时)
	 * 
	 * @param String
	 *            []
	 * @return String
	 * */
	public String jointSql(String[] arr, String expireDate) {
		int length = arr.length;
		StringBuffer value = new StringBuffer();
		value.append("('");
		for (int i = 0; i < length - 1; i++) {
			value.append(arr[i] + "','");
		}

		value.append(arr[length - 1] + "','" + getUUID() + "','" + expireDate
				+ "','0')");
		String insertValue = value.toString();
		return insertValue;
	}

	/**
	 * 把Map的Key/Value拼接成数据库的插入语句的形式
	 * 
	 * @param Map
	 * @return String
	 * */
	public String jointSql(Map<String, ?> map) {
		StringBuffer strKey = new StringBuffer();
		strKey.append("(");
		StringBuffer strValue = new StringBuffer();
		strValue.append("('");
		int i = 0;
		int length = map.size();
		for (Entry<String, ?> entry : map.entrySet()) {
			if (i < length - 1) {
				strKey.append(entry.getKey()).append(",");
				strValue.append(entry.getValue().toString()).append("','");
				i++;
			} else {
				strKey.append(entry.getKey()).append(
						",id,expire_time,extractflag)");
				strValue.append(entry.getValue().toString()).append("','")
						.append(getUUID()).append("','','0')");
			}
		}
		String str = strKey.toString() + " value " + strValue.toString();

		return str;
	}

	/**
	 * 获取所有列名的数组
	 * 
	 * @param JsonArray
	 * @return String[]
	 * */
	public String[] getColumnName(JsonArray jsonArray) {
		Column column = new Column();
		int length = jsonArray.size();
		int i = 0;
		String[] str = new String[length];

		Iterator<?> it = jsonArray.iterator();
		while (it.hasNext()) {// 遍历JsonArray对象
			JsonElement e = (JsonElement) it.next();

			// JsonElement转换为Column对象
			column = gson.fromJson(e, Column.class);
			str[i] = column.getName();
			i++;
		}

		return str;
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
	 * 获取类中的所有属性和值
	 * 
	 * @return HashMap key:属性名 value:值
	 * */
	public HashMap<String, Object> getfield(Object obj) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		Object f = obj;
		// 获取f对象对应类中的所有属性域
		Field[] fields = f.getClass().getDeclaredFields();
		for (int i = 0, len = fields.length; i < len; i++) {
			// 对于每个属性，获取属性名
			String varName = fields[i].getName();
			try {
				boolean accessFlag = fields[i].isAccessible();
				fields[i].setAccessible(true);
				Object o;
				o = fields[i].get(f);

				//System.out.println("传入的对象中包含一个如下的变量：" + varName + " = " + o);
				map.put(varName, o);
				// 恢复访问控制权限
				fields[i].setAccessible(accessFlag);
			} catch (IllegalArgumentException ex) {
				ex.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

		}
		return map;
	}
}
