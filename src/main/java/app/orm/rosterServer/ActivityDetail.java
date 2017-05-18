package app.orm.rosterServer;

public class ActivityDetail {

	String name;// 名称
	String type;// 类型
	String state;// 状态
	String rosterName;// 名单名
	int notDailNum = 0;// 未呼出总数
	int estabNum = 0;// 坐席应答数
	int connectorRate = 0;// 应答率%
	int dialNum = 0;// 已呼出总数
	int dncNum = 0;// dnc总数
	String firstStartTime = null;// 最早开始时间
	String lastEndTime = null;// 最晚结束时间

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setRosterName(String rosterName) {
		this.rosterName = rosterName;
	}

	public void setNotDailNum(int notDailNum) {
		this.notDailNum = notDailNum;
	}

	public void setEstabNum(int estabNum) {
		this.estabNum = estabNum;
	}

	public void setConnectorRate(int connectorRate) {
		this.connectorRate = connectorRate;
	}

	public void setDailNum(int dialNum) {
		this.dialNum = dialNum;
	}

	public void setDncNum(int dncNum) {
		this.dncNum = dncNum;
	}

	public void setFirstStartTime(String firstStartTime) {
		this.firstStartTime = firstStartTime;
	}

	public void setLastEndTime(String lastEndTime) {
		this.lastEndTime = lastEndTime;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public String getState() {
		return this.state;
	}

	public String getRosterName() {
		return this.rosterName;
	}

	public int getNotDailNum() {
		return this.notDailNum;
	}

	public int getEstabNum() {
		return this.estabNum;
	}

	public int getConnectorRate() {
		return this.connectorRate;
	}

	public int getDailNum() {
		return this.dialNum;
	}

	public int getDncNum() {
		return this.dncNum;
	}

	public String getFirstStartTime() {
		return this.firstStartTime;
	}

	public String getLastEndTime() {
		return this.lastEndTime;
	}
}
