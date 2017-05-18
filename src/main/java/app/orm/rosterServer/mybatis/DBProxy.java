package app.orm.rosterServer.mybatis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;

import component.util.Util;

/**
 * 数据库操作类
 * 
 * @author sunlu
 * @version 1.0
 * */
public class DBProxy {
	SqlSessionTemplate sqlSession;
	SqlSession session;
	private InputStream in = null;
	DBUtil util = new DBUtil();
	Map<String, Object> map = new HashMap<String, Object>();

	public DBProxy() {
		// in = this.getClass().getClassLoader()
		// .getResourceAsStream("mybatis.xml");
		// SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder()
		// .build(in);
		// session = sessionFactory.openSession();
	}

	public void init() {
		Util.trace(this, "数据库的初始化 - sunlu:");

		session = sqlSession.getSqlSessionFactory().openSession();

		if (session == null) {
			Util.trace(this, "session注入失败");
		} else {
			Util.trace(this, "session注入不为null");
		}
	}

	public void setSqlSession(SqlSessionTemplate sqlSession) {
		this.sqlSession = sqlSession;
	}

	/**
	 * 查询数据库中是否已经存在rosterInfo表
	 * 
	 * @param String
	 *            表名
	 * @return boolean
	 * @throws Exception
	 * */
	public boolean selectWhetherTableExists(String tableName) throws Exception {
		String dbUrl = session.getConnection().getMetaData().getURL();
		String dbName = "";
		if (dbUrl.indexOf("?") > 0) {
			dbName = dbUrl.substring(dbUrl.lastIndexOf('/') + 1,
					dbUrl.lastIndexOf('?'));
		} else {
			dbName = dbUrl
					.substring(dbUrl.lastIndexOf('/') + 1, dbUrl.length());
		}

		Util.trace(this, "DataBase Name: %s", dbName);
		String sql = "SELECT count(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '"
				+ dbName + "' and TABLE_NAME = '" + tableName + "'";
		map.put("sql", sql);

		Util.trace(this, "selectWhetherTableExists:%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectTableSql";
		int temp = ((Integer) session.selectOne(statement, map)).intValue();
		if (temp > 0) {
			Util.trace(this, "The Table %s is already exists !", tableName);
			return true;
		} else {
			Util.trace(this, "The Table %s is not exists, Please Create !",
					tableName);
			return false;
		}
	}

	/**
	 * 创建表
	 * 
	 * @param String
	 *            表名
	 * @param String
	 *            列名及列数据类型
	 * @return null
	 * @throws Exception
	 * */
	public void createTable(String tableName, String columns) throws Exception {
		String sql = "create table " + tableName + " " + columns;
		map.put("sql", sql);
		Util.trace(this, "createTable(String tableName, String columns):%s",
				sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.createTableSql";
		session.update(statement, map);
		Util.trace(this, "Table %s has been Created !", tableName);
	}

	/**
	 * 创建表
	 * 
	 * @param String
	 *            完整的sql语句
	 * @return null
	 * @throws Exception
	 * */
	public void createTable(String sql) throws Exception {
		map.put("sql", sql);
		Util.trace(this, "createTable(String sql):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.createTableSql";
		session.update(statement, map);
		Util.trace(this, "Table has been Created !");
	}

	/**
	 * 删除表
	 * 
	 * @param String
	 *            表名
	 * @return null
	 * @throws Exception
	 * */
	public void dropTable(String tableName) throws Exception {
		String sql = "drop table " + tableName;
		map.put("sql", sql);
		Util.trace(this, "dropTable(String tableName):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.deleteTableSql";
		session.update(statement, map);
		Util.trace(this, "Table %s has been Droped !", tableName);
	}

	/**
	 * 删除表中数据
	 * 
	 * @param String
	 *            表名
	 * @param String
	 *            条件
	 * @return null
	 * @throws Exception
	 * */
	public void delete(String tableName, String condition) throws Exception {
		String sql = "delete from " + tableName + " where " + condition;
		map.put("sql", sql);
		Util.trace(this, "delete(String tableName, String limit):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.deleteFromTableSql";
		session.delete(statement, map);
		session.commit();
	}

	/**
	 * 向表中插入多条数据(接口传入文件路径时使用)
	 * 
	 * @param String
	 *            表名及字段名
	 * @param String
	 *            要插入的数据组成的vallue字串
	 * @return null
	 * @throws Exception
	 * */
	public void insertFile(String name, String value) throws Exception {
		String sql = "insert into " + name + " values " + value;
		map.put("sql", sql);
		Util.trace(this, "insertFile:%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.insert(statement, map);
		session.commit();
	}

	/**
	 * 向rosterInfo表中插入单行数据(字段顺序不确定)
	 * 
	 * @param String
	 * @return null
	 * @throws Exception
	 * */
	public void insertJson(String value) throws Exception {
		String sql = "insert into " + value;
		map.put("sql", sql);
		Util.trace(this, "insertJson(String value):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.insert(statement, map);
		session.commit();
	}

	/**
	 * 向表中插入数据
	 * 
	 * @param String
	 *            完整的sql语句
	 * @return null
	 * @throws Exception
	 * */
	public void insert(String sql) throws Exception {
		map.put("sql", sql);
		Util.trace(this, "insert(String sql):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.insert(statement, map);
		session.commit();
	}

	/**
	 * 更新数据
	 * 
	 * @param String
	 *            完整的sql语句
	 * @return null
	 * @throws Exception
	 * */
	public void update(String sql) throws Exception {
		map.put("sql", sql);
		Util.trace(this, "update(String sql):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.update(statement, map);
		session.commit();
	}

	/**
	 * 删除表中数据
	 * 
	 * @param String
	 *            完整的sql语句
	 * @return null
	 * @throws Exception
	 * */
	public void delete(String sql) throws Exception {
		map.put("sql", sql);
		Util.trace(this, "delete(String sql):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.deleteFromTableSql";
		session.delete(statement, map);
		session.commit();
	}

	/**
	 * 更新指定表中的指定数据
	 * 
	 * @param String
	 *            表名
	 * @param String
	 *            更新的数据
	 * @param String
	 *            限制条件
	 * @return null
	 * @throws Exception
	 * */
	public void update(String tableName, String value, String condition)
			throws Exception {
		String sql = "update " + tableName + " set " + value + " where "
				+ condition;
		Util.trace(this,
				"update(String tableName, String value, String condition):%s",
				sql);
		map.put("sql", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.update(statement, map);
		session.commit();
	}

	/**
	 * 按条件查询表中数据
	 * 
	 * @param String
	 *            表名
	 * @param String
	 *            限制条件
	 * @return List
	 * @throws Exception
	 * */
	public List<?> select(String TableName, String condition) throws Exception {
		String sql = "SELECT * from " + TableName + " where " + condition + "";
		map.put("sql", sql);
		Util.trace(this, "select(String TableName, String condition):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectByNameSql";
		List<?> temp = session.selectList(statement, map);

		return temp;
	}

	/**
	 * 查询表的字段名
	 * 
	 * @param String
	 *            表名
	 * @return String[]
	 * @throws Exception
	 * */
	public String[] selectTableColumn(String tableName) throws Exception {
		String sql = "select COLUMN_NAME from information_schema.COLUMNS where table_name = '"
				+ tableName + "'";
		map.put("sql", sql);
		Util.trace(this, "selectTableColumn(String tableName):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectTableColumnsSql";
		List<String> temp = new ArrayList<String>();
		temp = session.selectList(statement, map);

		int size = temp.size();
		String[] arr = (String[]) temp.toArray(new String[size]);
		return arr;
	}

	/**
	 * 传入查询的SQL语句，返回查询的所有数据
	 * 
	 * @param String
	 *            完整的sql语句
	 * @return
	 * @return AbstractRoster
	 * @throws Exception
	 * */
	public List<Object> selectSql(String sql) throws Exception {
		map.put("sql", sql);
		Util.trace(this, "selectSql(String sql): %s", sql);
		List<Object> temp = new ArrayList<Object>();
		String statement2 = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectAllSql";

		temp = session.selectList(statement2, map);
		if (temp != null)
			Util.trace(this, "Result size: " + temp.size());
		else
			Util.trace(this, "Result is Null: ");
		return temp;
	}

	/**
	 * 查询表中所有数据
	 * 
	 * @param String
	 *            完整的sql语句
	 * @return
	 * @return AbstractRoster
	 * @throws Exception
	 * */
	public int selectCountSql(String sql) throws Exception {
		map.put("sql", sql);
		Util.trace(this, "selectCountSql(String sql):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectTableSql";
		int temp = ((Integer) session.selectOne(statement, map)).intValue();

		return temp;
	}

	/**
	 * 删除表中数据
	 * 
	 * @param String
	 *            表名
	 * @param Object
	 *            要删除的对象:按id删除
	 * @return null
	 * @throws Exception
	 * */
	public void delete(String tableName, Object obj) throws Exception {
		String id = (String) util.getfield(obj).get("id");
		String sql = "delete from " + tableName + " where id='" + id + "'";
		map.put("sql", sql);
		Util.trace(this, "delete(String tableName, Object obj):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.deleteFromTableSql";
		session.delete(statement, map);
		session.commit();
	}

	/**
	 * 向表中插入数据
	 * 
	 * @param String
	 *            表名
	 * @param Object
	 *            要插入的对象
	 * @return null
	 * @throws Exception
	 * */
	public void insert(String tableName, Object obj) throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map = util.getfield(obj);
		String value = " (";
		String column = " (";
		for (String key : map.keySet()) {
			if (map.get(key) != null) {
				column = column + "`" + key + "`,";
				value = value + "'" + map.get(key) + "',";
			}
		}
		column = column.substring(0, column.lastIndexOf(",")) + ")";
		value = value.substring(0, value.lastIndexOf(",")) + ")";
		String sql = "";
		sql = "insert into " + tableName + column + "value" + value;

		map.put("sql", sql);
		Util.trace(this, "insert(String tableName, Object obj):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.insert(statement, map);
		session.commit();
	}

	/**
	 * 更新指定表中的指定数据
	 * 
	 * @param String
	 *            表名
	 * @param Object
	 *            更新的对象:按id更新
	 * @return null
	 * @throws Exception
	 * */
	public void update(String tableName, Object obj) throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map = util.getfield(obj);
		String id = (String) map.get("id");

		String value = "";
		for (String key : map.keySet()) {
			if (map.get(key) != null) {
				value = value + "`" + key + "`='" + map.get(key) + "',";
			}
		}
		value = value.substring(0, value.lastIndexOf(","));

		String sql = "update " + tableName + " set " + value + " where id='"
				+ id + "'";

		Util.trace(this, "update(String tableName, Object obj):%s", sql);
		String statement = "app.orm.rosterServer.mybatis.RosterInfoMapper.insertSql";
		session.update(statement, map);
		session.commit();
	}

	/**
	 * 查询表中所有数据
	 * 
	 * @param String
	 *            表名
	 * @return List
	 * @throws Exception
	 * */
	public List<Object> select(String tableName) throws Exception {
		String sql = "SELECT * from " + tableName;
		Util.trace(this, "select(String tableName):%s", sql);
		map.put("sql", sql);
		List<Object> temp = new ArrayList<Object>();
		String statement2 = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectAllSql";
		temp = session.selectList(statement2, map);

		return temp;
	}

	/**
	 * 按条件对象中不为null的字段查询表中所有数据
	 * 
	 * @param String
	 *            表名
	 * @return List
	 * @throws Exception
	 * */
	public List<Object> selectCondition(String tableName, Object obj)
			throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map = util.getfield(obj);

		String value = "";
		for (String key : map.keySet()) {
			if (map.get(key) != null) {
				value = value + "`" + key + "`='" + map.get(key) + "' and ";
			}
		}
		value = value.substring(0, value.lastIndexOf("and"));

		String sql = "SELECT * from " + tableName + " where " + value;
		Util.trace(this, "selectCondition(String tableName, Object obj):%s",
				sql);
		map.put("sql", sql);
		List<Object> temp = new ArrayList<Object>();
		String statement2 = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectAllSql";
		temp = session.selectList(statement2, map);

		return temp;
	}

	/**
	 * 查询表中数据量
	 * 
	 * @param String
	 *            表名
	 * @return int 多少条数据
	 * @throws Exception
	 * */
	public int selectCount(String tableName) throws Exception {
		String sql = "SELECT count(*) from " + tableName;
		map.put("sql", sql);
		Util.trace(this, "selectCount(String tableName):%s", sql);
		String statement2 = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectTableSql";
		int temp = ((Integer) session.selectOne(statement2, map)).intValue();

		return temp;
	}

	/**
	 * 按对象中不为null字段查询表中数据量
	 * 
	 * @param String
	 *            表名
	 * @return int 多少条数据
	 * @throws Exception
	 * */
	public int selectCountContion(String tableName, Object obj)
			throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map = util.getfield(obj);

		String value = "";
		for (String key : map.keySet()) {
			if (map.get(key) != null) {
				value = value + "`" + key + "`='" + map.get(key) + "' and ";
			}
		}
		value = value.substring(0, value.lastIndexOf("and"));

		String sql = "SELECT count(*) from " + tableName + " where " + value;
		map.put("sql", sql);
		Util.trace(this, "selectCountContion(String tableName, Object obj):%s",
				sql);
		String statement2 = "app.orm.rosterServer.mybatis.RosterInfoMapper.selectTableSql";
		int temp = ((Integer) session.selectOne(statement2, map)).intValue();

		return temp;
	}

	/**
	 * 关闭连接
	 * 
	 * @param null
	 * @return null
	 * */
	public void close() throws IOException {
		in.close();
		session.close();
	}
}
