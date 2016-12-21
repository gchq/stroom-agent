package stroom.agent.collect;

import stroom.agent.util.date.DateUtil;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.zip.StroomZipRepository;
import stroom.agent.util.zip.HeaderMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PullSFTPCollector implements Collector, HeaderMapPopulator {

	private final static StroomLogger LOGGER = StroomLogger
			.getLogger(PullSFTPCollector.class);

	private String remoteDir = null;
	private String remoteFile = null;
	private String feed = null;
	private SFTPTransfer sftpTransfer = null;
	private SFTPGetHandler sftpGetHandler = new SFTPGetHandlerDefault();
	private List<HeaderMapPopulatorEntry> headerMapPopulatorEntryList = new ArrayList<HeaderMapPopulatorEntry>();

	private Integer minExpectedMatches = 1;
	private Integer maxExpectedMatches = 1;
	private boolean delete = false;
	private boolean drop = false;
	private String renameSufix;

	@Override
	public void process(StroomZipRepository stroomZipRepository, Date dateFrom) {
		try {
			sftpTransfer.connect();

			LOGGER.info("process() - processing");

			sftpTransfer.transferFiles(stroomZipRepository, getRemoteDir(),
					getRemoteFile(), getMinExpectedMatches(),
					getMaxExpectedMatches(), getSftpGetHandler(), this,
					isDelete(), isDrop(), getRenameSufix());
		} finally {
			sftpTransfer.disconnect();
		}

	}

	@Override
	public void populateHeaderMap(HeaderMap headerMap) {
		doPopulateHeaderMap(headerMap);
		logExtract(headerMap);
	}

	private void logExtract(HeaderMap headerMap) {
		LOGGER.info("logExtract() - %s Extracted %s %s", feed,
				sftpTransfer.getHost(),
				headerMap.get(StroomAgentCollectConstants.HEADER_ARG_REMOTE_FILE));
	}

	protected void doPopulateHeaderMap(HeaderMap headerMap) {
		headerMap.put(StroomAgentCollectConstants.HEADER_ARG_FEED, feed);
		headerMap.put(StroomAgentCollectConstants.HEADER_ARG_COLLECT_TIME,
				DateUtil.createNormalDateTimeString());
		headerMap.put(StroomAgentCollectConstants.HEADER_ARG_REMOTE_SERVER,
				getSftpTransfer().getHost());
		for (HeaderMapPopulatorEntry headerMapPopulatorEntry : headerMapPopulatorEntryList) {
			headerMap.put(headerMapPopulatorEntry.getKey(),
					headerMapPopulatorEntry.getValue(headerMap));
		}
	}

	public List<HeaderMapPopulatorEntry> getHeaderMapPopulatorEntryList() {
		return headerMapPopulatorEntryList;
	}

	public void setHeaderMapPopulatorEntryList(
			List<HeaderMapPopulatorEntry> headerMapPopulatorEntryList) {
		this.headerMapPopulatorEntryList = headerMapPopulatorEntryList;
	}

	public final String getRemoteDir() {
		return remoteDir;
	}

	public final void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public final String getRemoteFile() {
		return remoteFile;
	}

	public final void setRemoteFile(String remoteFile) {
		this.remoteFile = remoteFile;
	}

	public final SFTPTransfer getSftpTransfer() {
		return sftpTransfer;
	}

	public final void setSftpTransfer(SFTPTransfer sftpTransfer) {
		this.sftpTransfer = sftpTransfer;
	}

	public SFTPGetHandler getSftpGetHandler() {
		return sftpGetHandler;
	}

	public void setSftpGetHandler(SFTPGetHandler sftpGetHandler) {
		this.sftpGetHandler = sftpGetHandler;
	}

	public final String getFeed() {
		return feed;
	}

	public final void setFeed(String feed) {
		this.feed = feed;
	}

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}
	
	public boolean isDrop() {
		return drop;
	}
	public void setDrop(boolean drop) {
		this.drop = drop;
	}

	public String getRenameSufix() {
		return renameSufix;
	}

	public void setRenameSufix(String renameSufix) {
		this.renameSufix = renameSufix;
	}

	public final void setExpectedMatches(Integer expectedMatches) {
		setMinExpectedMatches(expectedMatches);
		setMaxExpectedMatches(expectedMatches);
	}

	public final void setMinExpectedMatches(Integer minExpectedMatches) {
		this.minExpectedMatches = minExpectedMatches;
		if (maxExpectedMatches != null && minExpectedMatches != null) {
			if (maxExpectedMatches.longValue() < minExpectedMatches.longValue()) {
				maxExpectedMatches = null;
			}
		}
	}

	public final void setMaxExpectedMatches(Integer maxExpectedMatches) {
		this.maxExpectedMatches = maxExpectedMatches;
		if (maxExpectedMatches != null && minExpectedMatches != null) {
			if (minExpectedMatches.longValue() > maxExpectedMatches.longValue()) {
				minExpectedMatches = null;
			}
		}
	}

	public final Integer getMinExpectedMatches() {
		return minExpectedMatches;
	}

	public final Integer getMaxExpectedMatches() {
		return maxExpectedMatches;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + getFeed() + " "
				+ getSftpTransfer().getHost();
	}
}
