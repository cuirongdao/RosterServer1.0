package app.orm.rosterServer;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import component.util.Util;

/**
 * Roster 定时任务实现类
 * 
 * @author sunlu
 * @version 1.0
 * */
public class RosterImpl implements Job {
	private RosterMgr rostermgr = Util.getBean("RosterMgr", RosterMgr.class);
	String filePath = "c:/file.txt";
	String name = "Roster32";

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// TODO Auto-generated method stub

		Util.trace(this, "name:%s, filepath:%s", this.name, filePath);
		boolean flag = rostermgr.loadFromFile(name, filePath);
		if (flag)
			Util.trace(this, "The Job In RosterMgr has been Excute Success!");
		else
			Util.trace(this, "The Job In RosterMgr has been Excute Failure!");
	}

	public void setRosterMgr(RosterMgr rosterMgr) {
		this.rostermgr = rosterMgr;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public void setname(String name) {
		this.name = name;
	}

	public RosterMgr getRosterMgr() {
		return this.rostermgr;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public String getName() {
		return this.name;
	}

}
