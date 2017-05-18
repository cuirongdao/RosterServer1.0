package app.orm.rosterServer.connector;

import component.util.Util;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture; 
import io.netty.channel.ChannelInitializer; 
import io.netty.channel.ChannelOption; 
import io.netty.channel.EventLoopGroup; 
import io.netty.channel.nio.NioEventLoopGroup; 
import io.netty.channel.socket.SocketChannel; 
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder; 
import io.netty.handler.codec.http.HttpResponseEncoder; 

class HttpServerThread implements Runnable
{
	HttpServer server;
	HttpServerThread(HttpServer server)
	{
		this.server = server;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Util.error(this, e, "Start HttpServer Fail!");
		}
	}
	
}
public class HttpServer {
    Thread httpThread =null;
	int port =-1;
    
	boolean init(int port) throws Exception
	{
		this.port = port;
		httpThread = new Thread(new HttpServerThread(this));
		httpThread.setDaemon(true);
		httpThread.start();
		return true;
		
		
	}
	boolean start() throws Exception
	{
		 EventLoopGroup bossGroup = new NioEventLoopGroup();
	        EventLoopGroup workerGroup = new NioEventLoopGroup();
	        try {
	            ServerBootstrap b = new ServerBootstrap();
	            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
	                    .childHandler(new ChannelInitializer<SocketChannel>() {
	                                @Override
	                                public void initChannel(SocketChannel ch) throws Exception {
	                                    // server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码
	                                    ch.pipeline().addLast(new HttpResponseEncoder());
	                                    // server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码
	                                    ch.pipeline().addLast(new HttpRequestDecoder());
	                                    ch.pipeline().addLast("http-aggregator",  new HttpObjectAggregator(655360));
	                                    //ch.pipeline().addLast("compressor", new HttpContentCompressor());
	                                    ch.pipeline().addLast(new HttpServerInboundHandler());
	                                }
	                            }).option(ChannelOption.SO_BACKLOG, 128) 
	                    .childOption(ChannelOption.SO_KEEPALIVE, true);

	            ChannelFuture f = b.bind(port).sync();

	            f.channel().closeFuture().sync();
	        } finally {
	            workerGroup.shutdownGracefully();
	            bossGroup.shutdownGracefully();
	        }
		return true;
	}
}
