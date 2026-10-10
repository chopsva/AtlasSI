package com.icr.atlas.POSGateway;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;

import com.icr.atlas.ATLASGuiExtend;

public class AtlasPOSGatewayThread extends Thread {
	Socket POSSocket;
	
	public AtlasPOSGatewayThread(Socket theSocket) {
		// TODO Auto-generated constructor stub
		this.POSSocket=theSocket;
		System.out.println("socket:"+this.POSSocket);
	}

	public void run() {
		StringBuffer posBuffer=new StringBuffer();
		int blankSkip=3;
		try {
			this.POSSocket.setSoTimeout(5000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("attempt to read");
		String fromPOS="";
		try {
			InputStream in=this.POSSocket.getInputStream();
			byte[] lenBytes=new byte[4];
			int msgLength=0;
			int read=in.read(lenBytes);
			if (read!=4) {
				//no header?
				msgLength=0;
			} else {
				msgLength=((lenBytes[3]&0xff)<<24) 
						| ((lenBytes[2] & 0xff)<<16) 
						| ((lenBytes[1] & 0xff)<<8)
						| (lenBytes[0] & 0xff);
			}
			System.out.println("fromPOS len:"+msgLength);
			byte[] msgBytes=new byte[msgLength];
			int totalRead=0;
			while (totalRead<msgLength) {
				int n=in.read(msgBytes,totalRead,msgLength-totalRead);
				if (n==-1)  {
					System.out.println("closed b4 complete");
					break;
				}
				totalRead +=n;
			}
			fromPOS=new String(msgBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("FROM POS:"+fromPOS);
		//String toSend=ATLASGuiExtend.getInstance().processPOSMessage(posBuffer.toString());
		String toSend=ATLASGuiExtend.getInstance().processPOSMessage(fromPOS);
		System.out.println("To POS:"+toSend);
		//String toSend="<AtlasResponse> </AtlasResponse>";
		String sendMsg="<AtlasResponse>"+toSend+"</AtlasResponse>";
		System.out.println("sendMsg:"+sendMsg);
		try {
			DataOutputStream out=new DataOutputStream(this.POSSocket.getOutputStream());
			int myVal=sendMsg.length();
			int highB=myVal/256;
			int lowB=myVal-(highB*256);
			byte[] theLen=new byte[4];
			theLen[0]=(byte)lowB;
			theLen[1]=(byte)highB;
			theLen[2]=0;
			theLen[3]=0;
			System.out.println(" send to POS a");
			out.write(theLen,0,4);
			System.out.println(" send to POS b");
			out.writeBytes(sendMsg);
			System.out.println(" send to POS c");
			out.close();
			System.out.println(" send to POS d");
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
			return;
		}
		
		try{
			System.out.println("attempt close socket");
			this.POSSocket.close();
			System.out.println("               closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
