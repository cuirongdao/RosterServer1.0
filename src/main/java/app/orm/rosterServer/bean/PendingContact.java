package app.orm.rosterServer.bean;

import java.util.Date;

import component.orm.Contact;

public class PendingContact extends Contact {
	int onThePhone;
	int resultCode;
	String reason;
	String callId;
	String callStartTime;
	String callEndTime;

	public void setOnThePhone(int onThePhone) {
		this.onThePhone = onThePhone;
	}

	public int getOnThePhone() {
		return this.onThePhone;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public int getResultCode() {
		return this.resultCode;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getReason() {
		return this.reason;
	}

	public void setCallId(String callId) {
		this.callId = callId;
	}

	public String getCallId() {
		return this.callId;
	}

	public void setCallStartTime(String callStartTime) {
		this.callStartTime = callStartTime;
	}

	public String getCallStartTime() {
		return this.callStartTime;
	}

	public void setCallEndTime(String callEndTime) {
		this.callEndTime = callEndTime;
	}

	public String getCallEndTime() {
		return this.callEndTime;
	}
}
