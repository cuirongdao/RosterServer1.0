package app.orm.rosterServer.connector;

import io.netty.buffer.ByteBuf;
import component.orm.protocol.ProtocolMgr;

public class HttpProtocolMgr extends ProtocolMgr{
	
	HttpServer server;
	int port ;
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	static HttpProtocolMgr _instance;
	public HttpServer getServer() {
		return server;
	}
	static HttpProtocolMgr getInstance()
	{
		return _instance;
	}
	public void setServer(HttpServer server) {
		this.server = server;
	}
	public HttpProtocolMgr() 
	{
		
	}
	/**
	 * 单例模式 不合适，后面进行调整
	 * @return
	 */
	
	
	public boolean init() throws Exception
	{
		_instance = this;
		start();
		return true;
	}
	//start http server
	public boolean start() throws Exception
	{
		server.init(port);
		return true;
	}
	
	public boolean stop()
	{
		//server.stop();
		return true;
	}
	
	public String processHttpRequest(String uri,String buf)
	{
		if(this.getProcessor() instanceof HttpRequestProcessor )
		{
			HttpRequestProcessor httpReqeuestProcessor = (HttpRequestProcessor)this.getProcessor();
			return httpReqeuestProcessor.processHttpRequest(uri,buf);
		}
		return "";
	}
	
	
	
	
	
}
