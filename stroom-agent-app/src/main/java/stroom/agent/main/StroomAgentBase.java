package stroom.agent.main;

import stroom.agent.util.AbstractCommandLineTool;
import stroom.agent.util.logging.StroomLogger;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * stroom.agent.main.StroomAgent configFile=<PATH>
 */
public abstract class StroomAgentBase extends AbstractCommandLineTool {
	
	private final static StroomLogger LOGGER = StroomLogger.getLogger(StroomAgentBase.class); 
	
	/**
	 * Command line setters
	 */
	private String lockFile = "/tmp/StroomAgent.lock";
	private String configFile = null;
	private RandomAccessFile lockRandomAccessFile;
	private FileLock lockFileLock;
	private ApplicationContext applicationContext;
	

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
	public void setLockFile(String lockFile) {
		this.lockFile = lockFile;
	}
	
	public boolean lock() {
		try {
			File file = new File(lockFile);
			if (!file.isFile()) {
				file.createNewFile();
			}
			
			lockRandomAccessFile = new RandomAccessFile(file, "rw");
			lockFileLock = lockRandomAccessFile.getChannel().tryLock();
			
			if (lockFileLock == null) {
				lockRandomAccessFile.close();
				return false;
			}
			return true;
		} catch (Exception ex) {
			LOGGER.error("lock", ex);
		}
		return false;
	}
	
	public void unlock() {
		try {
			lockRandomAccessFile.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public abstract void doRun();
	
	
	
	@Override
	public void run() {
		try {

			if (configFile==null) {
				LOGGER.error("run() - You must provide configFile=<Config File>");
				return;
			} else {
				// Here we work around a spring assumption that the config location
				// is a Web App style one ... if user thinks they are absolute path
				// '/' then make it '//' see FileSystemXmlApplicationContext.getResourceByPath
				if (configFile.startsWith("/")) {
					if (!configFile.startsWith("//")) {
						configFile = "/" + configFile;
					}
				}
			}

			if (lock()) {
				LOGGER.info("run() - %s - Obtained exclusive lock .  Using config %s", lockFile, configFile);
			} else {
				LOGGER.error("run() - %s - Failed to obtain exclusive lock (File is in use)", lockFile);
				return;
			}
			
			
			
			applicationContext = new FileSystemXmlApplicationContext(configFile);
			
			doRun();
			
			
		} catch (Throwable th) {
			LOGGER.error("run() - Unhandled exception", th);
		} finally {
			unlock();
			LOGGER.info("run() - Released Lock %s", lockFile);
		}
		
		
		
		
	}

}
