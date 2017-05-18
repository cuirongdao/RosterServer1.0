package app.orm.rosterServer;

import java.util.HashMap;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.StdSchedulerFactory;

import component.util.Util;

public class JobMgr {

	Scheduler scheduler;
	HashMap<String, JobDetail> jobMap;

	JobMgr() {
		init();
	}

	protected boolean init() {
		Util.trace(this, " JobMgr init!!!");
		try {
			jobMap = new HashMap<String, JobDetail>();
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error(this, e, "JobMgr init Scheduler Fail! ");
		}
		return true;

	}

	// 添加新任务
	public boolean NewJob(String name, String group, String cronSchedule,
			String cronEndTime, Job jobObject) {
		Util.trace(this, " JobMgr newJob. name:%s,group:%s,cronSchedule:%s",
				name, group, cronSchedule);
		JobDetail job = JobBuilder.newJob(JobImpl.class)
				.withIdentity(name, group).build();
		job.getJobDataMap().put("Job", jobObject);
		job.getJobDataMap().put("EndTime", cronEndTime);

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name, group)
				.withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
				.startNow().build(); // "0 0/1 * * * ?" 每分钟触发一次

		// 把作业和触发器注册到任务调度中
		try {
			scheduler.scheduleJob(job, trigger);
			jobMap.put(group + name, job);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error(this, e, "New Job Fail!");
			return false;
		}
		return true;
	}

	// 删除任务
	public boolean deleteJob(String name, String group) {
		Util.trace(this, " JobMgr deleteJob： %s", group + name);
		if (jobMap != null && jobMap.containsKey(group + name)) {
			JobDetail job = jobMap.get(group + name);
			try {
				scheduler.deleteJob(job.getKey());
				jobMap.remove(group + name);// 任务删除后，从map中移除
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				Util.error(this, e, "Delete Job Fail!");
				return false;
			}
		}
		return false;
	}

	// 暂停任务
	public boolean pauseJob(String name, String group) {
		Util.trace(this, " JobMgr pauseJob： %s", group + name);
		if (jobMap != null && jobMap.containsKey(group + name)) {
			JobDetail job = jobMap.get(group + name);
			try {
				scheduler.pauseJob(job.getKey());

				return true;
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				Util.error(this, e, "Pause Job Fail!");
				return false;
			}
		}
		return false;
	}

	// 继续任务
	public boolean resumeJob(String name, String group) {
		Util.trace(this, " JobMgr resumeJob： %s", group + name);
		if (jobMap != null && jobMap.containsKey(group + name)) {
			JobDetail job = jobMap.get(group + name);
			try {
				scheduler.resumeJob(job.getKey());

				return true;
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				Util.error(this, e, "Resume Job Fail!");
				return false;
			}
		}
		return false;
	}

	// 任务是否存在
	public boolean existsJob(String name, String group) {
		Util.trace(this, " JobMgr existsJob： %s", group + name);
		if (jobMap != null && jobMap.containsKey(group + name)) {
			return true;
		}
		return false;
	}

	// 查询任务状态
	public String getJobState(String name, String group) {
		Util.trace(this, " JobMgr existsJob： %s", group + name);
		if (jobMap != null && jobMap.containsKey(group + name)) {
			JobDetail job = jobMap.get(group + name);

			TriggerState state = null;
			try {
				state = scheduler.getTriggerState(scheduler
						.getTriggersOfJob(job.getKey()).get(0).getKey());

				System.out.println(state.toString());

				return state.toString();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return "NONE";
		}
		return group;
	}
}
