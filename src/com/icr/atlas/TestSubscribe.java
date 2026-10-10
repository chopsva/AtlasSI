package com.icr.atlas;

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.retail.AEF.automation.TransactionTotals;
import com.ibm.retail.AEF.event.CouponEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.PointsTotal;
import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.AEF.event.WorkstationStatusEvent;
import com.icr.atlas.eventPublishers.AtlasCouponEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasDiscountEventPublisher;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;
import com.icr.atlas.eventPublishers.AtlasItemSalesEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;
import com.icr.atlas.eventPublishers.AtlasTenderEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionTotalsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasWorkstationStatusEventPublisher;

public class TestSubscribe implements AtlasSubscriber<Object> {

	public TestSubscribe() {
		// TODO Auto-generated constructor stub
		System.out.println("TestSubscribe starting");
		AtlasCustomerEventPublisher.getInstance().subscribe(this);
		AtlasItemSalesEventPublisher.getInstance().subscribe(this);
		AtlasTransactionStatusEventPublisher.getInstance().subscribe(this);
		AtlasTenderEventPublisher.getInstance().subscribe(this);
		AtlasWorkstationStatusEventPublisher.getInstance().subscribe(this);
		AtlasCouponEventPublisher.getInstance().subscribe(this);
		AtlasOperatorEventPublisher.getInstance().subscribe(this);
		AtlasTransactionTotalsEventPublisher.getInstance().subscribe(this);
		AtlasDiscountEventPublisher.getInstance().subscribe(this);
	}

	@Override
	public void update(Object o) {
		// TODO Auto-generated method stub
		System.out.println("SUBSCRIBE GOT:"+o.getClass().getName());
		if (o instanceof CustomerEvent) {
			handleCustomerEvent((CustomerEvent)o);
		}
		if (o instanceof AtlasItemSalesEvent) {
			handleAtlasItemSalesEvent((AtlasItemSalesEvent)o);
		}
		if (o instanceof TransactionStatusEvent) {
			handleTransactionStatusEvent((TransactionStatusEvent)o);
		}
		if (o instanceof TenderEvent) {
			handleTenderEvent((TenderEvent)o);
		}
		if (o instanceof WorkstationStatusEvent) {
			handleWorkstationStatusEvent((WorkstationStatusEvent)o);
		}
		if (o instanceof CouponEvent) {
			handleCouponEvent((CouponEvent)o);
		}
		if (o instanceof OperatorEvent) {
			handleOperatorEvent((OperatorEvent)o);
		}
		if (o instanceof TransactionTotalsEvent) {
			handleTransactionTotals((TransactionTotalsEvent)o);
		}
		if (o instanceof DiscountEvent) {
			handleDiscountEvent((DiscountEvent)o);
		}
	}
	
	private void handleCustomerEvent(CustomerEvent event) {
		System.out.println("TEST SUBSCRIBE GOT Customer EVENT!");
		//CustomerEvent event=(CustomerEvent)o;
		System.out.println("  event ID:"+event.getID());
		ArrayList pBals=(ArrayList) event.getPointsBalances();
		for(int i=0;i<pBals.size();i++) {
			PointsTotal pT=(PointsTotal)pBals.get(i);
			System.out.println("  event "+i+"Points:"+pT.getValue());
			System.out.println("  event "+i+"Type:"+pT.getType());
		}
	}
	
	private void handleAtlasItemSalesEvent(AtlasItemSalesEvent event) {
		System.out.println("TEST SUBSCRIBE GOT itemSale EVENT");
		System.out.println("   event type:"+event.getEventType());
		System.out.println("   event id:"+event.getItemSalesEvent().getItemID());
		System.out.println("   event unitprice:"+event.getItemSalesEvent().getUnitPrice());
		
	}
	
	private void handleTransactionStatusEvent(TransactionStatusEvent event) {
		System.out.println("TEST SUBSCRIBE transStats");
		System.out.println("   event type:"+event.getEventType());
	}
	
	private void handleTenderEvent(TenderEvent event) {
		System.out.println("TEST SUBSCRIBE tenderEvent");
		System.out.println("   eventType:"+event.getEventType());
		System.out.println("   ID:"+event.getTenderType());
		System.out.println("  amount:"+event.getAmount());
	}
	
	private void handleWorkstationStatusEvent(WorkstationStatusEvent event) {
		System.out.println("TEST SUBSCRIUBE worksnEvent");
		System.out.println("    eventType:"+event.getEventType());
		System.out.println("    trmstatus"+event.getTerminalStatus());
	}
	 private void handleCouponEvent(CouponEvent event) {
		 System.out.println("TEST SUBSCRIBE couponEvent");
		 System.out.println("   eventType:"+event.getEventType());
		 System.out.println("       value: "+event.getValue());
	 }
	 private void handleOperatorEvent(OperatorEvent event) {
		 System.out.println("TEST SUBSCRIBE operatorEvent");
		 System.out.println("   eventType:"+event.getEventType());
		 System.out.println("       "+event.getID()+"-"+event.getName());
	 }
	 private void handleTransactionTotals(TransactionTotalsEvent event) {
		 System.out.println("TEST SUBSCRIBE transactionTotals");
		 System.out.println("    amtDue:"+event.getAmountDue());
	 }
	 private void handleDiscountEvent(DiscountEvent event) {
		 System.out.println("TEST SUBSCRIBE discountEvent");
		 System.out.println("     eventType:"+event.getEventType());
		 System.out.println("      discType:"+event.getDiscountType());
		 System.out.println("     discAmt:"+event.getAmount());
		 
	 }
	 
}
