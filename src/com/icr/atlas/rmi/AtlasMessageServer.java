package com.icr.atlas.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AtlasMessageServer {

	public AtlasMessageServer() {
		// TODO Auto-generated constructor stub
		System.out.println("Starting AtlasMessageServer");
		try {
			AtlasMessageService service=new AtlasMessageServiceImpl();
			Registry registry=LocateRegistry.createRegistry(6260);
			registry.rebind("AtlasMessageService", service);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
