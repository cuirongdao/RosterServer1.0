package app.orm.rosterServer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class mainClass {

	public static void main(String[] args) throws Exception {
		// HttpProtocolMgr.getInstance().start();
		// ApplicationContext c=new
		// ClassPathXmlApplicationContext("classpath:spring.xml");
		// HttpProtocolMgr p=(HttpProtocolMgr)c.getBean("protocol-http");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH：mm：ss");
		Date date = new Date();
		System.out.println(date);
		System.out.println(sdf.format(date));
	}

}
