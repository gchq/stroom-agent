package stroom.agent.collect;

import stroom.agent.util.date.DateUtil;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.zip.StroomZipRepository;
import stroom.agent.util.zip.HeaderMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.StringUtils;

public class DayPullSFTPCollector extends PullSFTPCollector {

	private final static StroomLogger LOGGER = StroomLogger
			.getLogger(DayPullSFTPCollector.class);

	private String dateFile = null;
	private List<Date> dateFileData = null;
	private Integer minAge = 1;
	private Integer maxAge = 30;

	private Date workingDate = null;

	public final static String DATE_SUFFIX = "T00:00:00.000Z";
	public final static int DATE_LENGTH = "2000-01-01".length();

	public Date toDate(String dateString) {
		return new Date(DateUtil.parseNormalDateTimeString(dateString
				+ DATE_SUFFIX));
	}

	public String toString(Date date) {
		return DateUtil.createNormalDateTimeString(date.getTime()).substring(0,
				DATE_LENGTH);
	}

	public final static int MS_IN_DAY = 24 * 60 * 60 * 1000;

	public int daysOld(Date date) {

		long ms = new Date().getTime() - date.getTime();
		return (int) Math.floor((double) ms / MS_IN_DAY);
	}

	@Override
	public void populateHeaderMap(HeaderMap headerMap) {
		doPopulateHeaderMap(headerMap);
		logExtract(headerMap);
	}

	private void logExtract(HeaderMap headerMap) {
		LOGGER.info("logExtract() - %s %s Extracted %s %s", getFeed(),
				toString(workingDate), getSftpTransfer().getHost(),
				headerMap.get(StroomAgentCollectConstants.HEADER_ARG_REMOTE_FILE));
	}

	private void loadDateFile() {
		try {
			ArrayList<Date> dateList = new ArrayList<Date>();
			LineNumberReader lineNumberReader = new LineNumberReader(
					new InputStreamReader(new FileInputStream(dateFile)));
			String line;
			while ((line = lineNumberReader.readLine()) != null) {
				if (StringUtils.hasText(line)) {
					dateList.add(toDate(line));
				}
			}
			Collections.sort(dateList);
			if (dateList.size() > 0) {
				dateFileData = dateList;
			}
		} catch (FileNotFoundException fnfEx) {
			LOGGER.warn("loadDateFile() - No file %s", dateFile);
		} catch (IOException ioEx) {
			throw new RuntimeException(ioEx);
		}
	}

	private void commitDateFile() throws IOException {
		// Write a new file first then delete the old and rename
		// That way if the disk is full we just get an IO
		StringBuilder builder = new StringBuilder();
		for (Date date : dateFileData) {
			builder.append(toString(date));
			builder.append("\n");
		}
		FilePersistUtil.commitFile(dateFile, builder.toString());
	}

	private void fillDateFile() {
		Date today = new Date();
		Date lastDate = dateFileData.get(dateFileData.size() - 1);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(lastDate);
		while (calendar.getTime().getTime() < today.getTime()) {
			calendar.add(Calendar.DATE, 1);
			dateFileData.add(calendar.getTime());
		}
	}

	private boolean removeOldDates() {
		boolean removed = false;
		if (getMaxAge() != null) {
			Iterator<Date> dateIter = dateFileData.iterator();
			while (dateIter.hasNext()) {
				Date date = dateIter.next();
				if (daysOld(date) > getMaxAge()) {
					dateIter.remove();
					removed = true;
					LOGGER.info("removeOldDates() - %s giving up on %s",
							getFeed(), toString(date));
				}
			}
		}
		return removed;
	}

	@Override
	public void process(StroomZipRepository stroomZipRepository, Date dateFrom) {
		// Avoid connecting if we don't need to.
		boolean connected = false;
		try {
			// Load the date file
			loadDateFile();

			if (dateFileData == null) {
				if (dateFrom == null) {
					LOGGER.error("process() - %s Invalid dateFile %s",
							getFeed(), dateFile);
					return;
				} else {
					dateFileData = new ArrayList<Date>();
					dateFileData.add(dateFrom);
				}
			}

			fillDateFile();
			commitDateFile();

			if (dateFileData.size() == 0) {
				LOGGER.info(
						"process() - %s Nothing todo as no dates to process",
						getFeed());
				return;
			}

			Iterator<Date> dateListItr = dateFileData.iterator();

			while (dateListItr.hasNext()) {
				Date date = dateListItr.next();

				boolean commitDateFile = false;

				if (daysOld(date) >= getMinAge()) {
					workingDate = date;
					LOGGER.info("process() - %s %s processing", getFeed(),
							toString(date));

					// Lazy Connect
					if (!connected) {
						getSftpTransfer().connect();
						connected = true;
					}

					long initialCount = stroomZipRepository.getFileCount();

					try {

						if (getSftpTransfer().transferFiles(stroomZipRepository,
								getRemoteDir(date), getRemoteFileRegEx(date),
								getMinExpectedMatches(),
								getMaxExpectedMatches(), getSftpGetHandler(),
								this, isDelete(), isDrop(), getRenameSufix())) {

							dateListItr.remove();

							long nowCount = stroomZipRepository.getFileCount();

							LOGGER.info(
									"process() - %s %s complete and created %s zip entries",
									getFeed(), toString(date), nowCount
											- initialCount);

							commitDateFile = true;
						} else {
							LOGGER.debug("process() - %s %s too new",
									getFeed(), toString(date));
						}

					} catch (Exception ex) {
						LOGGER.error("process() - Failed to process %s %s",
								getFeed(), toString(date), ex);

					}
				}

				// Do this out side the catch so as if it fails we abort the
				// whole process.
				if (commitDateFile) {
					commitDateFile();
				}

			}
			if (getMaxAge() != null) {
				if (removeOldDates()) {
					commitDateFile();
				}
			}

		} catch (IOException ioEx) {
			// This should only be from commitDateFile which is serious !
			throw new RuntimeException(ioEx);

		} finally {
			if (connected) {
				getSftpTransfer().disconnect();
			}
		}

	}

	public String getRemoteDir(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getRemoteDir());
		return simpleDateFormat.format(date);
	}

	public String getRemoteFileRegEx(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				getRemoteFile());
		return simpleDateFormat.format(date);
	}

	public String getDateFile() {
		return dateFile;
	}

	public void setDateFile(String pendingDateFile) {
		this.dateFile = pendingDateFile;
	}

	public Integer getMinAge() {
		return minAge;
	}

	public void setMinAge(Integer minAge) {
		if (minAge == null || minAge < 0) {
			throw new RuntimeException("min age must be a positive value");
		}
		this.minAge = minAge;
	}

	public Integer getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

}
