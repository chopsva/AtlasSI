package com.icr.atlas.eventPublishers;

public class AtlasScannerEventPublisher extends AtlasPublisher {
	private static AtlasScannerEventPublisher instance;
	
	public static AtlasScannerEventPublisher getInstance() {
		if (instance==null) 
			instance=new AtlasScannerEventPublisher();
		
		return instance;
	}
	
	public AtlasScannerEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	public void scannerData(String scannerData) {
		//
		publishIfExists("scannerData",scannerData); 
	}
	
}
