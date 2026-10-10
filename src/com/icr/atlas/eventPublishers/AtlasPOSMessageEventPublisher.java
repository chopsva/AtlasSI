package com.icr.atlas.eventPublishers;

import com.icr.atlas.AtlasUtils;

public class AtlasPOSMessageEventPublisher extends AtlasPublisher {

	private static AtlasPOSMessageEventPublisher instance;
	
	public static AtlasPOSMessageEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasPOSMessageEventPublisher();
		}
		
		return instance;
	}
	
	
	private AtlasPOSMessageEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	public void receivePOSMessage(String theMessage) {
		//don't just send the whole msg to everybody
		String ageEntry=AtlasUtils.getXML(theMessage, "AgeEntry");
		if (ageEntry!="") {
			publishIfExists("ageEntry",ageEntry);
		}
	}
	
}
