package com.icr.atlas.eventPublishers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.retail.AEF.event.POSAppEvent;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.rmi.AtlasMessageService;



public class AtlasPublisher {

	private final List<AtlasSubscriber<?>> subscribers=new CopyOnWriteArrayList<>();
	
	public AtlasPublisher() {
		// TODO Auto-generated constructor stub
	}
	
	public void subscribe(AtlasSubscriber<?> s) {
		subscribers.add(s);
	}
	public void unsubscribe(AtlasSubscriber<?> s) {
		subscribers.remove(s);
	}
	public void publish(Object o) {
		for(AtlasSubscriber<?> s : subscribers) {
			s.update(o);
		}
	}
	public void publishIfExists(String methodName,Object theEventObj) {
		System.out.println("publishIfExists("+methodName+")");
		for(AtlasSubscriber<?> s: subscribers) {
			Class<?> cls=s.getClass();
			Method[] methods=cls.getMethods();
			//System.out.println("methods:"+methods.length);
			for(Method m: methods) {
				//System.out.println("checking "+m.getName());
				if (m.getName().equals(methodName)) {
					try {
						m.invoke(s, theEventObj);
						break;
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	public void sendListenerEvent(POSAppEvent event) {
		if (!ATLASGuiExtend.getInstance().onSecondJVM()) {
			AtlasMessageService svc=ATLASGuiExtend.getInstance().getAtlasMessageService();
			if (svc!=null) {
				try {
					svc.sendListenerEvent(event);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void sendItemSalesEvent(AtlasItemSalesEvent event) {
		if (!ATLASGuiExtend.getInstance().onSecondJVM()) {
			AtlasMessageService svc=ATLASGuiExtend.getInstance().getAtlasMessageService();
			if (svc!=null) {
				try {
					svc.sendItemSalesEvent(event);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
