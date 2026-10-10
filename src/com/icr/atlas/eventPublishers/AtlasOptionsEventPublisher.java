package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.OptionsEvent;
import com.ibm.retail.AEF.event.OptionsListener;

public class AtlasOptionsEventPublisher extends AtlasPublisher implements OptionsListener{

	private static AtlasOptionsEventPublisher instance=null;
	public static AtlasOptionsEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasOptionsEventPublisher();
		}
		return instance;
		
	}
	
	private AtlasOptionsEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void optionsLoaded(OptionsEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		//System.out.println("Store Options:"+arg0.getStoreOptions());
		//System.out.println("Store Def:"+arg0.getStoreDefinition());
	}

}
