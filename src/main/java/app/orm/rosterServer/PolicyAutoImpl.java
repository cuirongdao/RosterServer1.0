package app.orm.rosterServer;

import component.orm.Activity;
import component.orm.Contact;
import component.orm.Policy;
import component.util.Util;

public class PolicyAutoImpl extends Policy {

	public boolean isSendOutReach(Contact contact, Activity activity) {
//		PendingContact pcontact= (PendingContact) contact;
		ActivityImpl actImpl = (ActivityImpl) activity;
		Util.trace(this, "dialMap.size:%d ,maxCapacity:%d",
				actImpl.dialMap.size(), actImpl.getMaxCapacity());
		if (actImpl.dialMap.size() < actImpl.getMaxCapacity()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isActivityComplete(ActivityImpl activity) {
		return false;
	}

}
