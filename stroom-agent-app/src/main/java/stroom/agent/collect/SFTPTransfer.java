package stroom.agent.collect;

import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.logging.LogExecutionTime;
import stroom.agent.util.shared.ModelStringUtil;
import stroom.agent.util.zip.StroomZipOutputStream;
import stroom.agent.util.zip.StroomZipRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SFTPTransfer {

	private final static StroomLogger LOGGER = StroomLogger
			.getLogger(SFTPTransfer.class);

	public final static int PERMISSION_DENIED = 3;
	public final static int NOSUCH_FILE = 2;

	private String user = null;
	private String host = null;
	private String knownHosts = null;
	private String identity = null;
	/**
	 * Only look for files and directories based on their age
	 */
	private Long validFileMTimeMs = null;
	private Long validFileATimeMs = null;
	private Long validDirMTimeMs = null;
	private Long validDirATimeMs = null;

	private Session session = null;
	private ChannelSftp channelSftp = null;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setValidFileMTime(String validFileAgeMs) {
		this.validFileMTimeMs = ModelStringUtil
				.parseDurationString(validFileAgeMs);
	}

	public void setValidFileATime(String validFileAgeMs) {
		this.validFileATimeMs = ModelStringUtil
				.parseDurationString(validFileAgeMs);
	}

	public void setValidDirMTime(String validFileAgeMs) {
		this.validDirMTimeMs = ModelStringUtil
				.parseDurationString(validFileAgeMs);
	}

	public void setValidDirATime(String validFileAgeMs) {
		this.validDirATimeMs = ModelStringUtil
				.parseDurationString(validFileAgeMs);
	}

	public String getKnownHosts() {
		return knownHosts;
	}

	public void setKnownHosts(String knownHosts) {
		this.knownHosts = knownHosts;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public void connect() {
		if (session != null) {
			throw new IllegalStateException("Already Connected");
		}
		try {
			JSch jSch = new JSch();
			if (knownHosts != null) {
				jSch.setKnownHosts(knownHosts);
			}
			if (identity != null) {
				jSch.addIdentity(identity);
			}
			session = jSch.getSession(user, host, 22);
			Properties properties = new Properties();
			properties.setProperty("StrictHostKeyChecking", "no");
			properties.setProperty("PreferredAuthentications", "publickey");
			session.setConfig(properties);
			session.connect();
			channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

			LOGGER.info("connect() - Connected to %s", host);
		} catch (Exception ex) {
			// Failed to connect
			session = null;
			LOGGER.error("connect() - Failed to connect to %s - %s ", host, ex.getMessage());
			throw new RuntimeException(ex);
		}
	}

	public void disconnect() {
		if (channelSftp != null) {
			channelSftp.disconnect();
			channelSftp = null;
		}
		if (session != null) {
			session.disconnect();
			session = null;
		}
		LOGGER.info("connect() - Disconnected from %s", host);
	}

	public boolean looksLikeRegEx(String path) {
		return path.contains("*") || path.contains(".") || path.contains("(")
				|| path.contains("[");
	}
	
	public InputStream getRemoteStream(SFTPFileDetails fileDetails) throws SftpException {
		return channelSftp.get(fileDetails.getPath());
	}
	
	public void delete(SFTPFileDetails fileDetails) throws SftpException {
		LOGGER.info("delete() - rm %s", fileDetails);
		channelSftp.rm(fileDetails.getPath());
	}

	public void rename(SFTPFileDetails remoteFile, String renameSufix) throws SftpException {
		String path = remoteFile.getPath();
		String newPath = path + renameSufix;
		LOGGER.info("rename() - mv %s %s", path, newPath);
		channelSftp.rename(path, newPath);
	}
	
	

	public List<SFTPFileDetails> buildFilesToTransfer(String remoteDir,
			String pattern) throws SftpException {
		List<SFTPFileDetails> filesToTransfer = new ArrayList<SFTPFileDetails>();

		String[] dirParts = remoteDir.substring(1).split("/");

		List<String> dirsToScan = new ArrayList<String>();
		dirsToScan.add("");

		for (String dirPart : dirParts) {
			// Not a wild card
			if (!looksLikeRegEx(dirPart)) {
				for (int i = 0; i < dirsToScan.size(); i++) {
					dirsToScan.set(i, dirsToScan.get(i) + "/" + dirPart);
				}
			} else {
				// Wild card
				Pattern subDirPattern = Pattern.compile(dirPart);
				List<String> newDirsToScan = new ArrayList<String>();

				for (int i = 0; i < dirsToScan.size(); i++) {
					List<SFTPFileDetails> subDirs = lsDir(dirsToScan.get(i));
					for (SFTPFileDetails subDir : subDirs) {
						if (subDirPattern.matcher(subDir.getName()).matches()) {
							for (String dirToScan : dirsToScan) {
								newDirsToScan.add(dirToScan + "/"
										+ subDir.getName());
							}
						}

					}
				}
				dirsToScan = newDirsToScan;

			}
		}

		// Now Scan all the directories for matching files
		for (String dirToScan : dirsToScan) {
			List<SFTPFileDetails> fileList = lsFile(dirToScan);

			for (SFTPFileDetails fileName : fileList) {
				if (fileName.getName().matches(pattern)) {
					filesToTransfer.add(fileName);
				}
			}
		}
		return filesToTransfer;
	}

	public boolean isDir(String targetDir) throws SftpException {
		try {
			SftpATTRS attrs = channelSftp.stat(targetDir);
			return attrs.isDir();
		} catch (SftpException sftpException) {
			return false;
		}
	}
	public boolean isFile(String targetPath) throws SftpException {
		try {
			channelSftp.stat(targetPath);
			return true;
		} catch (SftpException sftpException) {
			return false;
		}
	}
	
	public boolean mkdirs(String targetDir) throws SftpException {
		if (!isDir(targetDir)) {
			String[] parts = targetDir.split("/");
			StringBuilder path = new StringBuilder();
			for (String part : parts) {
				if (part.length()>0) {
					path.append("/");
					path.append(part);
				
					if (!isDir(path.toString())) {
						mkdir(path.toString());
					}
				}
			}
			return true;
		}
		return false;
	}
	public boolean mkdir(String targetDir) throws SftpException {
		LOGGER.info("mkdir() - %s", targetDir);
		channelSftp.mkdir(targetDir);
		return true;
	}

	
	public boolean transferFile(File sourceFile, SFTPFileDetails targetFile) throws SftpException, IOException {
		LOGGER.info("transferFile() - %s %s", sourceFile.getCanonicalFile(), targetFile.getPath());
		final String lockSuffix = ".lock";
		final String finalFileName = targetFile.getDir() + "/" + targetFile.getName();
		final String lockFileName = finalFileName + lockSuffix;
		// If the files already exist this could be bad !
		// safe thing todo is to quit and not overwrite them !!
		if (isFile(finalFileName)) {
			LOGGER.error("transferFile() - file already exists %s", finalFileName);
			throw new RuntimeException("File already exists " + finalFileName);
		}
		if (isFile(lockFileName)) {
			LOGGER.error("transferFile() - removing file %s", lockFileName);
			throw new RuntimeException("File already exists " + lockFileName);
		}
		FileInputStream fileInputStream = new FileInputStream(sourceFile);
		channelSftp.put(fileInputStream, lockFileName);
		channelSftp.rename(lockFileName, finalFileName);
		return true;
		
	}


	public boolean transferFiles(StroomZipRepository stroomZipRepository,
			String remoteDir, String pattern, Integer minExpectedMatches,
			Integer maxExpectedMatches, SFTPGetHandler sftpGetHandler,
			HeaderMapPopulator headerMapPopulator, boolean delete, boolean drop, final String renameSufix) {
		try {

			List<SFTPFileDetails> filesToTransfer = buildFilesToTransfer(remoteDir,
					pattern);

			if (minExpectedMatches != null
					&& filesToTransfer.size() < minExpectedMatches.intValue()) {
				LOGGER.warn(
						"transferFiles() - %s %s Min expected matches %s, actual matching %s, %s",
						remoteDir, pattern, minExpectedMatches,
						filesToTransfer.size(), filesToTransfer);
				return false;
			}
			if (maxExpectedMatches != null
					&& filesToTransfer.size() > maxExpectedMatches.intValue()) {
				LOGGER.warn(
						"transferFiles() - %s %s Max expected matches %s, actual matching %s, %s",
						remoteDir, pattern, maxExpectedMatches,
						filesToTransfer.size(), filesToTransfer);
				return false;
			}

			if (filesToTransfer.size() > 0) {
				StroomZipOutputStream stroomZipOutputStream = null;
				try {

					// Just drop the file and don't process?
					if (!drop) {
						stroomZipOutputStream = stroomZipRepository
						.getStroomZipOutputStream();
						
						int count = 1;
						for (SFTPFileDetails remoteFile : filesToTransfer) {
							LogExecutionTime executionTime = new LogExecutionTime();
							LOGGER.info("transferFiles() - %s %s", remoteFile,
									ModelStringUtil.formatByteSizeString(remoteFile
											.getSize()));
							count = sftpGetHandler.getFile(
									this,
									stroomZipOutputStream,
									headerMapPopulator,
									remoteFile, count);
							LOGGER.info("transferFiles() - %s completed in %s",
									remoteFile, executionTime);
						}
	
						stroomZipOutputStream.close();
						stroomZipOutputStream = null;
					}

					// Delete the remote files ?
					if (delete) {
						for (SFTPFileDetails remoteFile : filesToTransfer) {
							delete(remoteFile);
						}

					} else {
						if (StringUtils.hasText(renameSufix)) {
							for (SFTPFileDetails remoteFile : filesToTransfer) {
								rename(remoteFile, renameSufix);
							}
							
						}
					}
					

				} finally {
					// Some kind of error
					if (stroomZipOutputStream != null) {
						stroomZipOutputStream.closeDelete();
					}
				}
			}
			return true;

		} catch (SftpException sftpException) {
			if (sftpException.id == NOSUCH_FILE) {
				LOGGER.warn("transferFiles() - %s %s No such file", remoteDir,
						pattern);
				return false;
			}
			if (sftpException.id == PERMISSION_DENIED) {
				LOGGER.warn("transferFiles() - %s %s Permission denied",
						remoteDir, pattern);
				return false;
			}
			LOGGER.error("transferFiles() - %s %s some other error",
					remoteDir, pattern, sftpException);
			return false;
			
			

		} catch (IOException ioException) {
			throw new RuntimeException(ioException);
		}
	}

	private HashMap<String, List<SFTPFileDetails>> lsFileCache = new HashMap<String, List<SFTPFileDetails>>();
	private HashMap<String, List<SFTPFileDetails>> lsDirCache = new HashMap<String, List<SFTPFileDetails>>();

	private List<SFTPFileDetails> lsFile(String remoteDir) {

		if (lsFileCache.containsKey(remoteDir)) {
			return lsFileCache.get(remoteDir);
		}
		buildLsCache(remoteDir);
		return lsFileCache.get(remoteDir);
	}

	private List<SFTPFileDetails> lsDir(String remoteDir) {

		if (lsDirCache.containsKey(remoteDir)) {
			return lsDirCache.get(remoteDir);
		}
		buildLsCache(remoteDir);
		return lsDirCache.get(remoteDir);
	}

	public final static String THIS_DIR = ".";
	
	private boolean checkLsEntry(String dir, ChannelSftp.LsEntry file) {

		String fileName = file.getFilename();
		
		// Ignore hidden files but not this dir
		if (fileName.startsWith(".") && !fileName.equals(THIS_DIR)) {
			return false;
		}
		
		
		long mTime = file.getAttrs().getMTime() * 1000L;
		long aTime = file.getAttrs().getATime() * 1000L;

		long mTimeAge = System.currentTimeMillis() - mTime;
		long aTimeAge = System.currentTimeMillis() - aTime;

		if (file.getAttrs().isDir()) {
			// Dir
			if (validDirMTimeMs != null && mTimeAge < validDirMTimeMs) {
				LOGGER.info(
						"checkLsEntry() - Dir %s/%s is too new due to mtime %s < %s ",
						dir, file.getFilename(),
						ModelStringUtil.formatDurationString(aTimeAge),
						ModelStringUtil.formatDurationString(validDirATimeMs));
				return false;
			}
			if (validDirATimeMs != null && aTimeAge < validDirATimeMs) {
				LOGGER.info(
						"checkLsEntry() - Dir %s/%s is too new due to atime %s < %s ",
						dir, file.getFilename(),
						ModelStringUtil.formatDurationString(aTimeAge),
						ModelStringUtil.formatDurationString(validDirATimeMs));
				return false;
			}

		} else {
			// File
			if (validFileMTimeMs != null && mTimeAge < validFileMTimeMs) {
				LOGGER.info(
						"checkLsEntry() - File %s/%s is too new due to mtime %s < %s ",
						dir, file.getFilename(),
						ModelStringUtil.formatDurationString(aTimeAge),
						ModelStringUtil.formatDurationString(validFileMTimeMs));
				return false;
			}
			if (validFileATimeMs != null && aTimeAge < validFileATimeMs) {
				LOGGER.info(
						"checkLsEntry() - File %s/%s is too new due to atime %s < %s ",
						dir, file.getFilename(),
						ModelStringUtil.formatDurationString(aTimeAge),
						ModelStringUtil.formatDurationString(validFileATimeMs));
				return false;
			}
		}
		return true;

	}
	
	
	private ChannelSftp.LsEntry locateEntry(Vector<ChannelSftp.LsEntry> list, String name) {
		for (ChannelSftp.LsEntry file : list) {
			if (file.getFilename().equals(name)) {
				return file;
			}
		}
		return null;
		
	}

	private void buildLsCache(String dir) {
		List<SFTPFileDetails> fileList = new ArrayList<SFTPFileDetails>();
		List<SFTPFileDetails> dirList = new ArrayList<SFTPFileDetails>();
		lsFileCache.put(dir, fileList);
		lsDirCache.put(dir, dirList);

		LogExecutionTime executionTime = new LogExecutionTime();
		
		try {
			@SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> list = channelSftp.ls(dir);

			ChannelSftp.LsEntry thisDir = locateEntry(list, THIS_DIR); 
			
			// Only process the listing if the directory is OK (not too new etc)
			if (thisDir==null || checkLsEntry(dir, thisDir)) {
				
				for (ChannelSftp.LsEntry file : list) {
	
					if (thisDir != file && checkLsEntry(dir, file)) {
	
						String filename = file.getFilename();
	
						if (file.getAttrs().isDir()) {
							dirList.add(new SFTPFileDetails(dir, filename, file
									.getAttrs().getSize()));
						} else {
							fileList.add(new SFTPFileDetails(dir, filename, file
									.getAttrs().getSize()));
						}
					}
				}
			}
		} catch (SftpException sftpException) {
			if (sftpException.id == NOSUCH_FILE) {
				LOGGER.debug("buildLsCache() - %s No such file", dir);
			}
			if (sftpException.id == PERMISSION_DENIED) {
				LOGGER.warn("buildLsCache() - %s Permission denied", dir);
			}
		}
		LOGGER.info(
				"buildLsCache() - %s completed in %s with %s files and %s dirs",
				dir, executionTime, fileList.size(), dirList.size());

	}

}
