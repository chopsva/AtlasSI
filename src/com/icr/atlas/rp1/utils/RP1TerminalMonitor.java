package com.icr.atlas.rp1.utils;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.json.JSONObject;

import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.CashReceiptEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.ReceiptLineImpl;
import com.ibm.retail.si.util.AEFException;
import com.ibm.sureview.application.POSData;
import com.ibm.sureview.application.StoreOptions;
import com.ibm.sureview.application.SureviewApplication;
import com.ibm.sureview.application.UserData;
//import com.ibm.sureview.sc.application.SCData;
//import com.ibm.sureview.sc.application.SCSureviewApplication;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.eventPublishers.AtlasAEFPropertyChangeEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCashReceiptEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;


public class RP1TerminalMonitor implements AtlasSubscriber<Object>  {
	
	private static RP1TerminalMonitor instance;
	
	private final LineQueue printLines=new LineQueue();
	private final LineQueue displayLines=new LineQueue();
	private String lastLine1="";
	private String lastSentLine="====-=0-980876987676r747 6we";
	
    public enum ImageFormat {
        JPG,
        PNG
    }

    private static final String ENDPOINT ="/api/termmonitor.php";
    private static final long CAPTURE_INTERVAL_MS=1000L;
    private Thread monitorThread=null;
    private volatile boolean monitorRunning=false;
    private int termID;
    private volatile long monitorStopTime= 0L;
    private int testcount=0;
    //private static final long DEFAULT_MONITOR_TIME_MS=5L * 60L * 1000L;
    private static final long DEFAULT_MONITOR_TIME_MS=5L * 60L * 1000L;
    
    public static synchronized RP1TerminalMonitor getInstance() {
    	if (instance==null) {
    		instance=new RP1TerminalMonitor();
     	}
    	return instance;
    }
    
    //Atlas needs a constructor
    private RP1TerminalMonitor() {
    	System.out.println("starting RP1TerminalMonitor singleton");
    	termID=Integer.parseInt(ATLASGuiExtend.getInstance().getTermId());
    	AtlasAEFPropertyChangeEventPublisher.getInstance().subscribe(this);
    	AtlasCashReceiptEventPublisher.getInstance().subscribe(this);
    }
    
    
    
    /*
     * --------------------------------------------------------
     * Capture the complete desktop.
     * --------------------------------------------------------
     */
    public static BufferedImage captureScreen()
            throws AWTException {

        Rectangle screenBounds =getCompleteScreenBounds();
        Robot robot =new Robot();
        return robot.createScreenCapture(screenBounds);
    }

    /*
     * --------------------------------------------------------
     * Determine the complete desktop area.
     *
     * This will also handle multiple monitors by determining
     * the combined desktop rectangle.
     * --------------------------------------------------------
     */
    private static Rectangle getCompleteScreenBounds() {
        GraphicsEnvironment ge =GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        Rectangle result = null;
        for (int i = 0;i < screens.length;i++) {
            Rectangle bounds =screens[i].getDefaultConfiguration().getBounds();
            if (result == null) {
                result =new Rectangle(bounds);
            } else {
                result =result.union(bounds);
            }
        }
        if (result == null) {
            throw new IllegalStateException("No screen device found");
        }
        return result;
    }

    /*
     * --------------------------------------------------------
     * Resize an image.
     *
     * If width <= 0 and height <= 0:
     *      original size is returned.
     *
     * If only width is supplied:
     *      height is calculated preserving aspect ratio.
     *
     * If only height is supplied:
     *      width is calculated preserving aspect ratio.
     *
     * If both are supplied:
     *      image is resized to exactly those dimensions.
     * --------------------------------------------------------
     */
    public static BufferedImage resize(
            BufferedImage source,
            int width,
            int height) {

        if (width <= 0 && height <= 0) {
            return source;
        }
        int originalWidth = source.getWidth();
        int originalHeight =source.getHeight();
        if (width > 0 && height <= 0) {
            double ratio = (double) width/ (double) originalWidth;
            height =(int) Math.round(originalHeight* ratio);
        }
        if (height > 0 && width <= 0) {
            double ratio = (double) height/ (double) originalHeight;
            width =(int) Math.round(originalWidth * ratio);
        }
        if (width <= 0) {
            width = 1;
        }

        if (height <= 0) {
            height = 1;
        }
        /*
         * TYPE_INT_RGB is useful for JPG because
         * JPEG has no alpha channel.
         */
        BufferedImage resized =new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g =resized.createGraphics();

        try {
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.drawImage(source,0,0,width,height,null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    /*
     * --------------------------------------------------------
     * Encode screenshot to PNG or JPG.
     *
     * JPEG quality:
     *
     * 1.0 = highest quality / largest file
     * 0.8 = usually very good
     * 0.5 = more compressed
     * 0.2 = strongly compressed
     *
     * PNG ignores the quality argument.
     * --------------------------------------------------------
     */
    public static byte[] encodeImage(BufferedImage image,ImageFormat format,float quality)
            throws IOException {

        if (format == ImageFormat.PNG) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image,"png",output);
            return output.toByteArray();
        }
        /*
         * JPG
         */
        if (quality < 0.0f) {
            quality = 0.0f;
        }
        if (quality > 1.0f) {
            quality = 1.0f;
        }
        /*
         * Ensure JPG gets an RGB image.
         */
        BufferedImage rgbImage = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g =rgbImage.createGraphics();

        try {
            g.drawImage(image,0,0,null);
        } finally {
            g.dispose();
        }

        Iterator<ImageWriter> writers =ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPG ImageWriter available");
        }
        ImageWriter writer =writers.next();
        ByteArrayOutputStream output =new ByteArrayOutputStream();
        ImageOutputStream imageOutput =ImageIO.createImageOutputStream(output);
        try {
            writer.setOutput(imageOutput);
            ImageWriteParam params =writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
            }
            writer.write(null,new IIOImage(rgbImage,null,null),params);
            imageOutput.flush();
        } finally {
            writer.dispose();
            imageOutput.close();
        }
        return output.toByteArray();
    }


    /*
     * --------------------------------------------------------
     * Capture and encode the screenshot.
     * --------------------------------------------------------
     */
    public byte[] captureScreen(ImageFormat format,int width,int height,float quality)
            throws AWTException, IOException {

        BufferedImage image = captureScreen();
        if (width > 0 || height > 0) {
            image =resize(image,width,height);
        }
        drawRemainingSeconds(image);
        return encodeImage(image,format,quality);
    }


    /*
     * --------------------------------------------------------
     * Build the termTime value.
     *
     * Example:
     *
     * 2026-09-18T08:23:15.421
     * --------------------------------------------------------
     */
    private static String getTerminalTime() {
        SimpleDateFormat format =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return format.format(new Date());
    }


    /*
     * --------------------------------------------------------
     * Capture screen and send it to RP1.
     *
     * Payload:
     *
     * {
     *   "terminal" : 1,
     *   "termTime" : "2026-09-18T08:23:15.421",
     *   "screenData" : "....base64...."
     * }
     * --------------------------------------------------------
     */
    public ApiResponse sendScreen(
            int terminal,
            ImageFormat format,
            int width,
            int height,
            float quality)
            throws AWTException, IOException {

        /*
         * Capture and encode the image.
         */
        byte[] imageBytes =captureScreen(format,width,height,quality);
        /*
         * Convert binary JPG/PNG into Base64
         * so that it can be stored in JSON.
         */
        String screenData = Base64.getEncoder().encodeToString(imageBytes);
        JSONObject payload = new JSONObject();
        payload.put("terminal",terminal);
        payload.put("termTime",getTerminalTime());
        payload.put("screenData",screenData);
        payload.put("screenType", format == ImageFormat.JPG ? "jpg" :"png");
        payload.put("remaingSeconds", getRemainingMonitorSeconds());
        JSONObject more=new JSONObject();
        more.put("printLines", printLines.getLines());
        more.put("displayLines",displayLines.getLines());
        payload.put("additionalInfo", more);
        /*
         * Send through LAN relay.
         */
        return RP1RelayClient.getInstance().postJson(ENDPOINT,payload.toString(),true); //skipQueue=true
    }
    
    //TEST 
    public synchronized void startMonitoring(
    		final int terminal,
    		final ImageFormat format,
    		final int width,
    		final int height,
    		final float quality) {
    	System.out.println("RP1TerminalMonitor startMonitoring called");
    	
    	if (monitorThread !=null && monitorThread.isAlive()) {
    		System.out.println("already running reset stoptime");
    		setStopTimeMinutesFromNow(5);
    		return; 
    	}
    	monitorStopTime=System.currentTimeMillis()+DEFAULT_MONITOR_TIME_MS;
    	if (!monitorRunning) {
    		//clear the line queues since we were not running before
    		printLines.clearLines();
    		displayLines.clearLines();
    	}
    	
    	
    	monitorRunning=true;
        monitorThread =new Thread(new Runnable() {
                            @Override
                            public void run() {
                            	try {
	                                while (monitorRunning) {
	                                    try {
	                                        ApiResponse response = sendScreen(terminal,format,width,height,quality);
	                                        System.out.println("Screen monitor terminal " + terminal+ " status="+ response.getStatus());
	                                    } catch (Exception e) {
	                                        /*
	                                         * Don't allow one failed capture or
	                                         * network problem to kill the thread.
	                                         */
	                                        System.out.println("Screen monitor error: "+ e);
	                                    }
	
	                                    /*
	                                     * Only sleep if monitoring is
	                                     * still supposed to be running.
	                                     */
	                                    if (!monitorRunning) {
	                                        break;
	                                    }
	                                    if (getRemainingMonitorMillis()<=0) {
	                                    	monitorRunning=false;
	                                    	break;
	                                    }
	                                    
	                                    try {
	                                        Thread.sleep(CAPTURE_INTERVAL_MS);
	                                    } catch (InterruptedException e) {
	                                        if (!monitorRunning) {
	                                            break;
	                                        }
	                                    }
	                                } 
                            	} finally {
                            		monitorRunning=false;
                            		synchronized(RP1TerminalMonitor.this) {
                            			if (Thread.currentThread()==monitorThread) {
                            				monitorThread=null;
                            			}
                            		}
                            	}
                                System.out.println("Screen monitoring stopped");
                            }
                        });


        monitorThread.setName("TerminalScreenMonitor");
        /*
         * Daemon means this thread will not prevent
         * the JVM from exiting.
         */
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    public synchronized void stopMonitoring() {
    	monitorRunning=false;
    	monitorStopTime=0L;
    	if (monitorThread!=null) {
    		monitorThread.interrupt();
    		monitorThread=null;
    	}
    }
    
    public boolean isMonitoring() {
    	return monitorRunning;
    }
    public void extendMonitoring() {
    	setStopTimeMinutesFromNow(5);
    }
    public  void setStopTimeMinutesFromNow(int minutes) {
    	if (minutes<=0) {
    		stopMonitoring();
    		return;
    	}
    	monitorStopTime=System.currentTimeMillis() + ((long)minutes*60L *1000L);
    	Thread thread=monitorThread;
    	if (thread!=null) {
    		thread.interrupt();
    	}
    	
    }
    public void setStopTime(long stopTimeMillis) {
    	monitorStopTime=stopTimeMillis;
    	Thread thread=monitorThread;
    	if (thread!=null) {
    		thread.interrupt();
    	}
    }
    public long getStopTime() {
    	return monitorStopTime;
    }
    public long getRemainingMonitorMillis() {

        if (!monitorRunning) {
            return 0L;
        }

        long remaining =monitorStopTime- System.currentTimeMillis();

        return Math.max(0L,remaining);
    }    
    public long getRemainingMonitorSeconds() {
    	return getRemainingMonitorMillis()/1000L; 
    }
    private void drawRemainingSeconds(
            BufferedImage image) {

        long seconds = getRemainingMonitorSeconds();
        String text = seconds + "sec";
        Graphics2D g =image.createGraphics();
        try {
            g.setFont(new Font("Arial",Font.BOLD,18));
            FontMetrics fm = g.getFontMetrics();
            int padding = 6;
            int textWidth =fm.stringWidth(text);
            int textHeight = fm.getHeight();
            int x = image.getWidth() - textWidth- (padding * 2);
            int y = image.getHeight()- textHeight- (padding * 2);
            /*
             * Black background box.
             */
            g.setColor(new Color(0, 0, 0,170));
            g.fillRect(x,y,textWidth + (padding * 2),textHeight + (padding * 2));
            /*
             * White text.
             */
            g.setColor(Color.WHITE);

            g.drawString(text,x + padding,y + padding + fm.getAscent());
        } finally {
            g.dispose();
        }
    }

    //ATLAS CONNECTION
	@Override
	public void update(Object o) {
		if (o instanceof CashReceiptEvent) {
			handleCashReceiptEvent((CashReceiptEvent)o);
		}
		if (o instanceof AEFPropertyChangeEvent) {
			handleAEFPropertyChange((AEFPropertyChangeEvent)o);
		}
	}
		
	private void handleAEFPropertyChange(AEFPropertyChangeEvent arg0) {	
		// TODO Auto-generated method stub
		//debug
//		if (testcount>5 && SCSureviewApplication.getInstance()!=null) { 
//			StoreOptions sOpts=SCSureviewApplication.getInstance().getStoreOptions();
//			if (sOpts!=null) {
//				System.out.println("STOREOPS:"+sOpts.dumpTable());
//			}
//			UserData testData=SCSureviewApplication.getInstance().getUserData();
//			if (testData!=null) {
//				System.out.println("USERDATA:"+testData.dumpTable());
//			} else {
//				System.out.println("USERDATA IS NULL:");
//			}
//			
//			POSData pData=SCSureviewApplication.getInstance().getPOSData();
//			if (pData!=null) {
//				System.out.println("POSDATA:"+pData.dumpTable());
//			} else {
//				System.out.println("POSDATA NULL");
//			}
//			SCData scData=((SCSureviewApplication)SCSureviewApplication.getInstance()).getSCData();
//			if (scData!=null) {
//				System.out.println("SCDATA:"+scData.dumpTable());
//			} else {
//				System.out.println("SCDATA NULL");
//			}
//			testcount=0;
//		}
//		testcount +=1;
////		
		
		
		System.out.println("TM-prop:"+arg0.getCategoryName()+'/'+arg0.getPropertyName()+" now:"+arg0.getNewLogValue());
		if (!"POS_DEVICE".equals(arg0.getCategoryName())) {
			return;
		}
		System.out.println("POS_DEVICE FOUND");
		if ("ANPROMPT_LINE1".equals(arg0.getPropertyName())) {
			System.out.println("line1:"+arg0.getNewLogValue());
			lastLine1=arg0.getNewLogValue();
		}
		if ("ANPROMPT_LINE2".equals(arg0.getPropertyName())) {
			System.out.println("line2:"+arg0.getNewLogValue());
			String toSend=lastLine1 + arg0.getNewLogValue();
			if (!toSend.equals(lastSentLine) && !toSend.trim().isEmpty()) {
				displayLines.add(toSend);
				lastSentLine=toSend;
			}
			
		}
		
	}
	private void handleCashReceiptEvent(CashReceiptEvent arg0) {
		System.out.println("cashreceipt event:"+arg0);
		ReceiptLineImpl tmpLine=null;
		for(Object item : arg0.getPrintLineObjects()) {
			if (item instanceof ReceiptLineImpl) {
				tmpLine=(ReceiptLineImpl)item;
				if (!"PostPrintReceipt".equals(tmpLine.getLineCategory())) {
					System.out.println("printLine-"+tmpLine.getFilteredData());
					printLines.add(tmpLine.getFilteredData().replaceFirst(" +$",""));
				}
			}
		}
	}
	
	public String[] stringToArray(String value) {
		if (value==null){
			return new String[0];
		}
		
		value=value.trim();
		if (value.startsWith("[") && value.endsWith("]")) {
			value=value.substring(1,value.length()-1);
		}
		if (value.trim().isEmpty()) {
			return new String[0];
		}
		String[] result=value.split(",");
		return result;
		
	}

	public static class LineQueue {
		private static final int MAX_LINES=20;
		private final Deque<String> lines=new ArrayDeque<String>();
		public void add(String line) {
			if (lines.size()>=MAX_LINES) {
				lines.removeFirst();
			}
			lines.add(line);
		}
		public String[] getLines() {
			return lines.toArray(new String[lines.size()]);
		}
		public void clearLines() {
			lines.clear();
		}
	}
	
}