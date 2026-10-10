package com.icr.atlas.Dragonfruit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.OS4690.File4690;
import com.ibm.OS4690.FileOutputStream4690;

public final class DragonfruitLogger {

	public enum Level {
		NONE(1),
        ERROR(2),
        WARN(3),
        INFO(4),
        DEBUG(5);

        private final int value;

        Level(int v) {
            this.value = v;
        }

        public int getValue() {
            return value;
        }
    }
	
	private static final int MAX_INDEX=3;
	//private static final String BASE_NAME_PREFIX="r::C:\\silogs\\drgn"; //we know that dir will exist!
	private static final String BASE_NAME_PREFIX="r::C:\\silogs\\drgn"; //we know that dir will exist!
	private Level currentLevel=Level.INFO;
	
	private static volatile DragonfruitLogger INSTANCE;
	private static final Object INIT_LOCK= new Object();
	
	private final String identifier;
	//private final PrintWriter fileWriter;
	private FileOutputStream4690 fileOut;
	private final SimpleDateFormat timestampFormat=new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
	
	public static DragonfruitLogger init(String identifier) {
		if (INSTANCE==null) {
			synchronized(INIT_LOCK) {
				if (INSTANCE== null){
					INSTANCE=new DragonfruitLogger(identifier);
				}
			}
		}
		return INSTANCE;
	}
	
	public static DragonfruitLogger getInstance() {
		if (INSTANCE==null) {
			init("000");
		}
		return INSTANCE;
	}
	
	public void setLevel(Level level) {
		if (level!=null) {
			this.currentLevel=level;
			info("Level set to:"+level.name());
		}
		if (level==Level.NONE && fileOut!=null) {
			close(); //close any open file
		}
	}
	
	
	private DragonfruitLogger(String identifier) {
		// TODO Auto-generated constructor stub
		System.out.println("DragonfruitLogger called with:"+identifier);
		this.identifier=identifier;
		this.fileOut=createWriterWithRollover();
	}
	
	private FileOutputStream4690 createWriterWithRollover() {
		System.out.println("DragonfruitLogger opening file");
		String baseName=BASE_NAME_PREFIX+identifier;
		try {
			for(int i=MAX_INDEX; i>=0; i--) {
				File4690 f=new File4690(baseName+"."+i);
				if (f.exists()) {
					if (i==MAX_INDEX) {
						System.out.println("DragonfruitLogger deleting:"+f.getCanonicalPath());
						if (!f.delete()) {
							System.out.println("DragonfruitLogger: Failed to delete "+f.getCanonicalPath());
						}
					} else {
						System.out.println("DragonfruitLogger renaming:"+f.getCanonicalPath());
						File4690 dest=new File4690(baseName+"."+(i+1));
						if (!f.renameTo(dest)) {
							System.out.println("DragonfruitLogger: Failed to rename "+f.getCanonicalPath()+" to "+dest.getCanonicalPath());
							System.out.println("DragonfruitLogger: fromExists-"+f.exists());
							System.out.println("DragonfruitLogger: toExists-"+dest.exists());
							System.out.println("DragonfruitLogger: fromCanWrite-"+f.canWrite());
							System.out.println("DragonfruitLogger: toCanWrite-"+
										(dest.getParentFile()!=null && dest.getParentFile().canWrite()));
							
							
						}
					}
				}
			}
			File4690 logFile=new File4690(baseName+".0");
			System.out.println("DragonfruitLogger file:"+logFile.getCanonicalPath());
			//return new PrintWriter(new FileWriter(logFile,true),true);
			return new FileOutputStream4690(logFile,true);
		} catch (IOException e) {
			System.out.println("DragonfruitLogger: Failed to open log file:"+e.getMessage());
			return null;
		}
	}

	public void info(String msg) {
		log(Level.INFO,msg);
	}
	
	public void warn(String msg) {
		log(Level.WARN, msg);
	}
	
	public void error(String msg) {
		log(Level.ERROR, msg);
	}
	public void debug(String msg) {
		log(Level.DEBUG, msg);
	}

	public synchronized void log(Level level,String message) {
		if (level.getValue()>currentLevel.getValue()) {
			return;
		}
		
		String ts=timestampFormat.format(new Date());
		String output=String.format("%s [%s] %s\r\n", ts,level.name(),message);
		if (level==Level.ERROR || level==Level.WARN) {
			System.out.println(output);
		}
		if (fileOut!=null) {
			//fileWriter.println(output);
			try {
				fileOut.write(output.getBytes("UTF-8"));
				fileOut.flush();
			} catch (IOException e) {
				System.err.println("DragonLogger write failed:"+e.getMessage());
			}
		
			
		}
	}
	
	public synchronized void close() {
		if (fileOut!=null) {
			try {
				fileOut.flush();
				fileOut.close();
			} catch (Exception e) {
				//
			}
			fileOut=null;
		}
		
	}
			
}
