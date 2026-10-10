package com.icr.atlas.eventPublishers;

import com.ibm.retail.AEF.event.ItemSalesEvent;

public class AtlasItemSalesEvent implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;
	private ItemSalesEvent theItemSalesEvent;
	private String thisEventType;
	
	public AtlasItemSalesEvent() {
		// TODO Auto-generated constructor stub
		theItemSalesEvent=null;
		thisEventType="";
	}
	public AtlasItemSalesEvent(ItemSalesEvent e,String t) {
		this.theItemSalesEvent=e;
		this.thisEventType=t;
	}

	public ItemSalesEvent getItemSalesEvent() {
		return this.theItemSalesEvent;
	}
	
	public String getEventType() {
		return this.thisEventType;
	}
}
