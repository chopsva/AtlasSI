package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.WorkstationStatusEvent;
import com.ibm.retail.AEF.event.WorkstationStatusListener;

public class AtlasWorkstationStatusEventPublisher extends AtlasPublisher implements WorkstationStatusListener {
	private static AtlasWorkstationStatusEventPublisher instance;
	
	public static AtlasWorkstationStatusEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasWorkstationStatusEventPublisher();
		}
		return instance;
	}
	
	private AtlasWorkstationStatusEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void workstationStatusChanged(WorkstationStatusEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		publish(arg0);
		sendListenerEvent(arg0);
	}

}
