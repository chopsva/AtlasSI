package com.icr.atlas.POSGateway;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.icr.atlas.ATLASGuiExtend;

public class AtlasPOSGateway {
	
	public static int thePort=8675;
	private static AtlasPOSGateway instance=null;
	private static Log log=LogFactory.getLog(AtlasPOSGateway.class);
	
	public static AtlasPOSGateway getInstance() {
		if (instance==null) {
			instance=new AtlasPOSGateway();
		}
		return instance;
	}
	
	private AtlasPOSGateway() {
		// TODO Auto-generated constructor stub
		log.info("AtlasPOSGateway init");
		System.out.println("AtlasPOSGateway init");
		while(true) {
			try{
				watchThePort();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
	
	public void watchThePort() {
		ServerSocket serverSocket=null;
		boolean listening=true;
		
		int waitToWatch=5;
		int delayTime=5;
		System.out.println("AtlasPOSGateway a");
		while(serverSocket==null) {
			
				try {
					serverSocket=new ServerSocket(thePort);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					try {
						Thread.sleep(delayTime*1000);
					} catch (InterruptedException e1) {
						//dont' care
					}
					waitToWatch -=1;
					delayTime *=2;
					if (waitToWatch<1) {
						log.info("CAN NOT WATCH PORT!");  //add comm status update?
						break;
					}
				}
				
		}
		if (serverSocket==null) {
			return;
		}
		System.out.println("AtlasPOSGateway b "+thePort);
		log.info("WATCHING PORT "+thePort);
		while(listening) {
			try {
				new AtlasPOSGatewayThread(serverSocket.accept()).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	

}
