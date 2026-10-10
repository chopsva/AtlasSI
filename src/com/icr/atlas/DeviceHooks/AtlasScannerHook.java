package com.icr.atlas.DeviceHooks;

import com.ibm.retail.AEF.data.POSDataProviderImpl;
import com.ibm.retail.AEF.device.ScannerHook;
import com.ibm.retail.si.util.AEFException;
import com.icr.atlas.eventPublishers.AtlasScannerEventPublisher;

import jpos.JposException;
import jpos.events.DataEvent;

public class AtlasScannerHook extends ScannerHook {

	POSDataProviderImpl theDataProvider;
	String initStringParm;
	
	public AtlasScannerHook(POSDataProviderImpl arg0, String arg1) throws AEFException {
		super(arg0, arg1);

		this.theDataProvider=arg0;
		this.initStringParm=arg1;
		//nothing to do here we don't link to anybody
	}

	@Override
	public void fireDataEvent(DataEvent evt) {
		byte[] theData;
		try {
			theData = theDeviceService.getScanData();
			AtlasScannerEventPublisher.getInstance().scannerData(new String(theData));
		} catch (JposException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
