package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.DiscountListener;

public class AtlasDiscountEventPublisher extends AtlasPublisher implements DiscountListener {
	private static AtlasDiscountEventPublisher instance;

	public static AtlasDiscountEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasDiscountEventPublisher();
		}
		return instance;
	}
	
	private AtlasDiscountEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void discountApplied(DiscountEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		publish(arg0);
		sendListenerEvent(arg0);
	}

}
