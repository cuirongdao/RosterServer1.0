package app.orm.rosterServer;

import app.orm.rosterServer.bean.PendingContact;

import com.google.gson.Gson;

import component.redis.connection.RedisConnectionFactory;
import component.redis.connection.lettuce.LettuceConnectionFactory;
import component.redis.core.RedisTemplate;
import component.util.Util;

public class RediaOperator {

	RedisTemplate redisTemplate;
	RedisConnectionFactory redisConFactory;
	String statusPersistorUrl = "redis-cluster://172.16.2.31:7000,172.16.2.32:7001,172.16.2.33:7002,172.16.2.31:7003,172.16.2.32:7004,172.16.2.33:7005";

	public void init() {
		if (statusPersistorUrl == null || statusPersistorUrl.length() < 4) {
			Util.warn(this,
					"Redis URL format is wrong,please check the config!", "");
			return;
		}
		try {
			Util.info(this, "Init Redis 1... url:" + statusPersistorUrl, "");

			redisConFactory = LettuceConnectionFactory
					.create(statusPersistorUrl);
			redisTemplate = new RedisTemplate(redisConFactory);
			Util.info(this, "Init Redis end...", "");

		} catch (Exception e) {
			redisTemplate = null;
			Util.warn(this, "init Redis FAIL" + e.getMessage(), "");
		}
	}

	public boolean set(String key, String str) {
		Util.trace(this, "set key:%s, Data:%s", key, str);
		if (redisTemplate == null) {
			// Util.warn(this, "ReisTemplate is null", "");

			return false;
		}

		try {
			redisTemplate.set(key, str);
		} catch (Throwable e) {
			Util.warn(this, e, "Set Data Fail :%s", key);
			return false;
		}
		return true;
	}

	public PendingContact get(String key) {
		Util.info(this, "getInfo from Redis key:%s", key);
		if (redisTemplate == null)
			return null;
		String info = redisTemplate.get(key);
		if (info == null) {
			Util.info(this, "getInfo from Redis , null");
			return null;
		} else {
			Util.info(this, "getInfo from Redis , %s", info);
			Gson gson = new Gson();
			try {
				PendingContact pendingContact = gson.fromJson(info,
						PendingContact.class);
				return pendingContact;
			} catch (Exception e) {
				Util.error(this, e, "getInfo from redis fail!");
				return null;
			}
		}
	}

	public void delete(String key) {
		Util.info(this, "delete from Redis key:%s", key);
		if (redisTemplate == null)
			return;

		redisTemplate.delete(key);

	}
}
