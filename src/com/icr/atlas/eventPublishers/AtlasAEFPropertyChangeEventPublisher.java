package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.AEFPropertyChangeListener;

public class AtlasAEFPropertyChangeEventPublisher extends AtlasPublisher implements AEFPropertyChangeListener {
	private static AtlasAEFPropertyChangeEventPublisher instance;
	
	public static AtlasAEFPropertyChangeEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasAEFPropertyChangeEventPublisher();
		}
		return instance;
	}
	
	private AtlasAEFPropertyChangeEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void propertyChanged(AEFPropertyChangeEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		//System.out.println("prop:"+arg0.getCategoryName()+'/'+arg0.getPropertyName()+" now:"+arg0.getNewLogValue());
		publish(arg0);
	}

}
