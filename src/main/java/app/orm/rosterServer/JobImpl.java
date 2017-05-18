package app.orm.rosterServer;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import component.util.Util;

public class JobImpl implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		try{
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		Job object = (Job) dataMap.get("Job");
		object.execute(context);
		}catch(Exception ex)
		{
			Util.error(this, ex, "JobImpl execute Fail!");
		}
	}

}
