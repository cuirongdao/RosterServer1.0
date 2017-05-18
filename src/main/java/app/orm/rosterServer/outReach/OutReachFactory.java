package app.orm.rosterServer.outReach;

import component.orm.IOutReach;
import component.orm.OutReachType;
import component.util.Util;

/**
 * 外呼渠道 工厂，生产外呼代理，然后调用接口进行外呼
 * @author bizheng
 *
 */
public class OutReachFactory {
	static OutReachFactory instance;
	
	public static OutReachFactory getInstance()
	{
		if(instance ==null)
		{
			//synchronized(instance)
			{
				if(instance ==null)
					instance = new OutReachFactory();
			}
		}
		return instance;
	}
	
	protected OutReachFactory()
	{
		
	}
	
	public IOutReach getOutReachProxy(OutReachType type)
	{
		IOutReach outReach = null;
		if(type == OutReachType.call)
			outReach = Util.getBean("OutBoundProxy", OutBoundProxy.class);
		return outReach;
	}

}
