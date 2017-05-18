package app.orm.rosterServer;
import app.orm.rosterServer.connector.HttpProtocolMgr;
import app.orm.rosterServer.mybatis.DBProxy;
import component.service.RunMode;
import component.service.impl.AbstractService;
import component.util.UserException;
import component.util.Util;

public class RosterServer extends AbstractService {

	HttpProtocolMgr protocolMgr;
	
	public HttpProtocolMgr getProtocolMgr() {
		return protocolMgr;
	}

	public void setProtocolMgr(HttpProtocolMgr httpProtocolMgr) {
		this.protocolMgr = httpProtocolMgr;
	}
	

	@Override
	public String init(RunMode runMode) throws UserException 
	{
		Util.getBean("RosterMgr",RosterMgr.class);
		Util.getBean("PolicyMgr",PolicyMgr.class);
		Util.getBean("ActivityMgr",ActivityMgr.class);
		Util.getBean("dbProxy", DBProxy.class);
		
		
		return null;
	}
	
	@Override
	public void exit() {
	}
	
	@Override
	public void run() throws UserException
	{
		//connectorMgr.start();
		super.run();
		
	}
	
	
}
