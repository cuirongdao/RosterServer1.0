package app.orm.rosterServer.mybatis;

public class Column {
	String name = null;
	String type = null;
	
	public String getName(){
		return this.name;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void setType(String type){
		this.type = type;
	}
}
