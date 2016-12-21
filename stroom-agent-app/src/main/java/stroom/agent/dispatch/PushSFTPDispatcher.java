package stroom.agent.dispatch;

import stroom.agent.collect.SFTPFileDetails;
import stroom.agent.collect.SFTPTransfer;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.zip.StroomZipRepository;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.jcraft.jsch.SftpException;

/**
 * Class to dispatch a STROOM ZIP REPO over SSH  
 */
public class PushSFTPDispatcher implements Dispatcher {
	
	private final static StroomLogger LOGGER = StroomLogger.getLogger(PushSFTPDispatcher.class); 
	
	private SFTPTransfer sftpTransfer;
	/**
	 * The remote dir to use
	 */
	private String remoteDir = null;
	/**
	 * Create the remote dir if we can
	 */
	private boolean mkdir = false;
	/**
	 * Delete the files once transfered
	 * Normally one dispatcher does this
	 */
	private boolean readOnly = false;
	
	@Override
	public boolean isReadOnly() {
		return readOnly;
	}
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	@Override
	public boolean process(StroomZipRepository stroomZipRepository) {
		if (getRemoteDir()==null) {
			LOGGER.error("remoteDir not set");
			return false;
		}
		// The remote dir can have the date embedded
		final String remoteDirInstance = getRemoteDir(new Date());
		try {
			sftpTransfer.connect();

			// Check and Create Remote Dir
			if (!sftpTransfer.isDir(remoteDirInstance)) {
				if (isMkdir()) {
					sftpTransfer.mkdirs(remoteDirInstance);
				} else {	
					LOGGER.error("process() - remoteDir %s does not exist", remoteDirInstance);
					return false;
				}
			}
			
			// Keep track of all the paths we create so as not to keep querying them
			String rootPah = stroomZipRepository.getRootDir().getCanonicalPath();
			Set<String> pathsCreated = new HashSet<>();
			pathsCreated.add(remoteDirInstance);
			
			// Send each file
			for (File file : stroomZipRepository.getZipFiles()) {
				
				String subPath = file.getParentFile().getCanonicalPath().substring(rootPah.length());
				
				String fileRemoteDir = remoteDirInstance;
				if (subPath.length()>0) {
					fileRemoteDir = remoteDirInstance + "/" + subPath;
				}
				
				if (!pathsCreated.contains(fileRemoteDir)) {
					if(!sftpTransfer.isDir(fileRemoteDir)) {
						sftpTransfer.mkdirs(fileRemoteDir);
					}
					pathsCreated.add(fileRemoteDir);
				}
				
				
				// Transfer the file
				SFTPFileDetails remoteFile = new SFTPFileDetails(
						fileRemoteDir,
						file.getName(),
						file.length());
				
				sftpTransfer.transferFile(file, remoteFile);
				
				// Delete it so we don't send again
				if (!isReadOnly()) {
					LOGGER.info("process() - Removing processed file %s", file);
					file.delete();
				}
			}
		} catch (IOException ioEx) {
			throw new RuntimeException(ioEx);
		} catch (SftpException sftpEx) {
			throw new RuntimeException(sftpEx);
		} finally {
			sftpTransfer.disconnect();
		}
		
		return true;
	}
	
	
	public void setSftpTransfer(final SFTPTransfer sftpTransfer) {
		this.sftpTransfer = sftpTransfer;
	}
	public SFTPTransfer getSftpTransfer() {
		return sftpTransfer;
	}
	public void setRemoteDir(final String remoteDir) {
		this.remoteDir = remoteDir;
	}
	public String getRemoteDir() {
		return remoteDir;
	}
	public String getRemoteDir(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getRemoteDir());
		return simpleDateFormat.format(date);
	}

	public boolean isMkdir() {
		return mkdir;
	}
	public void setMkdir(final boolean mkdirRemoteDir) {
		this.mkdir = mkdirRemoteDir;
	}

}
