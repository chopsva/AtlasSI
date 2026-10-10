package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.CouponEvent;
import com.ibm.retail.AEF.event.CouponListener;

public class AtlasCouponEventPublisher extends AtlasPublisher implements CouponListener {
	private static AtlasCouponEventPublisher instance;
	
	public static AtlasCouponEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasCouponEventPublisher();
		}
		return instance;
	}
	
	public AtlasCouponEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void couponApplied(CouponEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		publish(arg0);
		sendListenerEvent(arg0);
	}

}
