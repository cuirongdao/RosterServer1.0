package app.orm.rosterServer.connector;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import app.orm.rosterServer.RosterMgr;

import com.google.gson.Gson;

import component.util.LogLevel;
import component.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

public class HttpServerInboundHandler extends ChannelInboundHandlerAdapter {
	private HttpRequest request;

	private static final HttpDataFactory factory = new DefaultHttpDataFactory(
			DefaultHttpDataFactory.MINSIZE);
	HttpPostRequestDecoder decoder;
	Map<String, String> requestParams = new HashMap<String, String>();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg instanceof HttpRequest) {
			request = (HttpRequest) msg;
			String uri = request.uri();
			FullHttpRequest req = (FullHttpRequest) msg;
			ByteBuf buf = req.content();
			String res = "";
			String strBuf = buf.toString(io.netty.util.CharsetUtil.UTF_8);
			if (uri.indexOf("UpLoad") > 0) {

				try {
					decoder = new HttpPostRequestDecoder(factory, request);
					decoder.offer((HttpContent) (req));
					strBuf = readHttpDataChunkByChunk(); // 从解码器decoder中读出数据

				} catch (ErrorDataDecoderException e1) {

					res = "";
				} catch (Exception e1) {
					res = "";
				}

			}

			if (HttpProtocolMgr.getInstance() != null)
				res = HttpProtocolMgr.getInstance().processHttpRequest(uri,
						strBuf);
			else
				res = "{\"code\":-1}";

			buf.release();
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
					OK, Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
			response.headers()
					.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
			response.headers().set(
					HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().set(CONTENT_LENGTH,
					response.content().readableBytes());
			if (HttpHeaders.isKeepAlive(request)) {
				response.headers().set(CONNECTION, Values.KEEP_ALIVE);
			}
			ctx.write(response);
			ctx.flush();

		}
		if (msg instanceof HttpContent) {
			// HttpContent content = (HttpContent) msg;

			/*
			 * ByteBuf buf = content.content(); String res = "";
			 * if(HttpProtocolMgr.getInstance()!=null) res =
			 * HttpProtocolMgr.getInstance
			 * ().processHttpRequest(buf.toString(io.netty
			 * .util.CharsetUtil.UTF_8)); else res="{\"code\":-1}";
			 * //System.out.
			 * println("buffer:"+buf.toString(io.netty.util.CharsetUtil.UTF_8));
			 * 
			 * buf.release(); FullHttpResponse response = new
			 * DefaultFullHttpResponse(HTTP_1_1, OK,
			 * Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
			 * response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
			 * "text/plain");
			 * response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN
			 * , "*"); response.headers().set(CONTENT_LENGTH,
			 * response.content().readableBytes()); if
			 * (HttpHeaders.isKeepAlive(request)) {
			 * response.headers().set(CONNECTION, Values.KEEP_ALIVE); }
			 * ctx.write(response); ctx.flush();
			 */
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		Util.log("", LogLevel.Error, "HttpServer Exception ", cause);

		ctx.close();
	}

	/**
	 * Example of reading request by chunk and getting values from chunk to
	 * chunk 从decoder中读出数据，写入临时对象，然后写入？ 这个封装主要是为了释放临时对象
	 */
	private String readHttpDataChunkByChunk() {

		try {
			while (decoder.hasNext()) {
				InterfaceHttpData data = decoder.next();
				if (data != null) {
					try {
						// new value
						writeHttpData(data);
					} finally {
						data.release();
					}
				}

				if (data == null) {
					Gson gson = new Gson();
					Util.trace(this, "gson.toJson(requestParams)1: %s",
							gson.toJson(requestParams));
					if (requestParams.containsKey("columns")) {
						Util.trace(this, "xxx");
						RosterMgr rosterMgr = Util.getBean("RosterMgr",
								RosterMgr.class);
						rosterMgr.processHandMake(requestParams);
					}
				}
			}

		} catch (EndOfDataDecoderException e1) {
			// end
			// responseContent.append("\r\n\r\nEND OF CONTENT CHUNK BY CHUNK\r\n\r\n");
			// Util.error(this, e1, "HttpUpLoad readHttpDataChunkByChunk Fail");

			Gson gson = new Gson();
			Util.trace(this, "gson.toJson(requestParams)2: %s",
					gson.toJson(requestParams));
//			if (requestParams.containsKey("columns")) {
				Util.trace(this, "zzz");
				RosterMgr rosterMgr = Util
						.getBean("RosterMgr", RosterMgr.class);
				rosterMgr.processHandMake(requestParams);
//			}
		}
		return "";
	}

	private void writeHttpData(InterfaceHttpData data) {
		// Attribute就是form表单里带的各种 name= 的属性

		String filePath = "c:/download" + File.separator;
		Util.trace(this, "filePath: %s", filePath);
		if (data.getHttpDataType() == HttpDataType.Attribute) {
			// 获取表单中的属性
			Attribute attribute = (Attribute) data;
			Util.trace(this, attribute.toString());
			Util.trace(this, attribute.getName());
			try {
				requestParams.put(attribute.getName(), attribute.getValue());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (data.getHttpDataType() == HttpDataType.InternalAttribute) {
			Util.trace(this, "writeHttpData: %s", data.getHttpDataType()
					.toString());
		} else {
			Util.trace(this, "writeHttpData: %s", data.getHttpDataType()
					.toString());
			String uploadFileName = getUploadFileName(data);
			FileUpload fileUpload = (FileUpload) data;
			if (fileUpload.isCompleted()) {
				File dir = new File(filePath);
				if (!dir.exists()) {
					dir.mkdir();
				}
				File dest = new File(dir, uploadFileName);
				try {
					fileUpload.renameTo(dest);
					filePath = dest.getPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Util.error(this, e, "Http UpLoad writeHttpData");
					// return "";
				}
				requestParams.put("filePath", filePath);
			}
		}
		// return filePath;
	}

	private String getUploadFileName(InterfaceHttpData data) {
		String content = data.toString();
		Util.trace(this, "content1: %s", content);
		String temp = content.substring(0, content.indexOf("\n"));
		content = temp.substring(temp.lastIndexOf("=") + 2,
				temp.lastIndexOf("\""));
		return content;
	}
}
