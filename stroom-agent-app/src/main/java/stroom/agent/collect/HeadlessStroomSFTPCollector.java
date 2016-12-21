package stroom.agent.collect;

import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.zip.StroomZipRepository;
import stroom.agent.util.zip.HeaderMap;

import java.util.Date;
import java.util.List;

import com.jcraft.jsch.SftpException;

public class HeadlessStroomSFTPCollector implements Collector, HeaderMapPopulator {

	private final static StroomLogger LOGGER = StroomLogger
			.getLogger(HeadlessStroomSFTPCollector.class);

	private String remoteDir = null;
	private String remoteFile = null;

	private SFTPTransfer sftpTransfer = null;
	private HeadlessStroomFileProcessor headlessStroomFileProcessor;
	private boolean delete = false;
	private String renameSufix;
	private String feedMatch;
	private String feedReplace;

	@Override
	public void process(StroomZipRepository stroomZipRepository, Date dateFrom) {
		try {
			sftpTransfer.connect();

			LOGGER.info("process() - processing");		
			
			List<SFTPFileDetails> fileList = sftpTransfer.buildFilesToTransfer(remoteDir, remoteFile);
			
			headlessStroomFileProcessor = new HeadlessStroomFileProcessor();
			headlessStroomFileProcessor.setFeedMatch(feedMatch.split(","));
			headlessStroomFileProcessor.setFeedReplace(feedReplace.split(","));			
			
			for (SFTPFileDetails fileDetails : fileList) {
				try {
					LOGGER.info("process() - %s", fileDetails);
					headlessStroomFileProcessor.processFile(sftpTransfer, stroomZipRepository, fileDetails, delete, renameSufix);
				} catch (Exception ex) {
					LOGGER.error("process()", ex);
				} 
			}
			
		} catch (SftpException sftpException) {
			LOGGER.error("process()", sftpException);
		} finally {
			sftpTransfer.disconnect();
		}

	}

	@Override
	public void populateHeaderMap(HeaderMap headerMap) {
		logExtract(headerMap);
	}

	private void logExtract(HeaderMap headerMap) {
		LOGGER.info("logExtract() - %s Extracted %s %s", "?",
				sftpTransfer.getHost(),
				headerMap.get(StroomAgentCollectConstants.HEADER_ARG_REMOTE_FILE));
	}


	public final String getRemoteDir() {
		return remoteDir;
	}

	public final void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}
	public String getRemoteFile() {
		return remoteFile;
	}
	public void setRemoteFile(String remoteFile) {
		this.remoteFile = remoteFile;
	}

	
	
	public final SFTPTransfer getSftpTransfer() {
		return sftpTransfer;
	}

	public final void setSftpTransfer(SFTPTransfer sftpTransfer) {
		this.sftpTransfer = sftpTransfer;
	}

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public String getRenameSufix() {
		return renameSufix;
	}

	public void setRenameSufix(String renameSufix) {
		this.renameSufix = renameSufix;
	}


	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + getFeed() + " "
				+ getSftpTransfer().getHost();
	}

	@Override
	public String getFeed() {
		String feed = null;
		if (headlessStroomFileProcessor != null) {
			feed = headlessStroomFileProcessor.getFeed();
		} 
		if (feed == null) {
			feed = "FEED_TBA";
		}
					
		return feed;
	}
	
	
	public void setFeedMatch(String feedMatch) {
		this.feedMatch = feedMatch;
	}
	public void setFeedReplace(String feedReplace) {
		this.feedReplace = feedReplace;
	}
	
}
