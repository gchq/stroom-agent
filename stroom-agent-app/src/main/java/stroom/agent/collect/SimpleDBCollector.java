package stroom.agent.collect;

import stroom.agent.util.date.DateUtil;
import stroom.agent.util.io.StreamUtil;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.shared.ModelStringUtil;
import stroom.agent.util.zip.StroomZipEntry;
import stroom.agent.util.zip.StroomZipFileType;
import stroom.agent.util.zip.StroomZipOutputStream;
import stroom.agent.util.zip.StroomZipRepository;
import stroom.agent.util.zip.HeaderMap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.util.StringUtils;

public class SimpleDBCollector implements Collector, HeaderMapPopulator {

	private final static StroomLogger LOGGER = StroomLogger
			.getLogger(SimpleDBCollector.class);

	private String query = null;
	private String fromDateQuery = null;
	private String jdbcDriver = null;
	private String jdbcUrl = null;
	private String jdbcPassword = null;
	private String jdbcUser = null;
	private String idPersistFile = null;
	private String feed;
	private boolean header = true;
	private boolean checkSequence = true;
	private boolean debug = false;
	private boolean stroomDate = true;
	private Long maxRows = null;
	private Integer loginTimeout = null;

	private List<HeaderMapPopulatorEntry> headerMapPopulatorEntryList = new ArrayList<HeaderMapPopulatorEntry>();

	public final static String HEADER_ARG_EXTRACT_START_ID = "ExtractStartId";
	public final static String HEADER_ARG_EXTRACT_END_ID = "ExtractEndId";
	public final static String HEADER_ARG_JDBC_URL = "JdbcUrl";

	public List<HeaderMapPopulatorEntry> getHeaderMapPopulatorEntryList() {
		return headerMapPopulatorEntryList;
	}

	public void setHeaderMapPopulatorEntryList(
			List<HeaderMapPopulatorEntry> headerMapPopulatorEntryList) {
		this.headerMapPopulatorEntryList = headerMapPopulatorEntryList;
	}

	public String getFeed() {
		return feed;
	}

	public void setFeed(String feed) {
		this.feed = feed;
	}

	public boolean isHeader() {
		return header;
	}

	public void setHeader(boolean header) {
		this.header = header;
	}

	public String getIdPersistFile() {
		return idPersistFile;
	}

	public void setIdPersistFile(String idPersistFile) {
		this.idPersistFile = idPersistFile;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getJdbcPassword() {
		return jdbcPassword;
	}

	public void setJdbcPassword(String jdbcPassword) {
		this.jdbcPassword = jdbcPassword;
	}

	public String getJdbcUser() {
		return jdbcUser;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public boolean isCheckSequence() {
		return checkSequence;
	}

	public void setCheckSequence(boolean checkSequence) {
		this.checkSequence = checkSequence;
	}

	public String getFromDateQuery() {
		return fromDateQuery;
	}

	public void setFromDateQuery(String fromDateQuery) {
		this.fromDateQuery = fromDateQuery;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isStroomDate() {
		return stroomDate;
	}

	public void setStroomDate(boolean stroomDate) {
		this.stroomDate = stroomDate;
	}

	public void setMaxRows(Long maxRows) {
		this.maxRows = maxRows;
	}

	public Long getMaxRows() {
		return maxRows;
	}
	
	public void setLoginTimeout(String loginTimeout) {
		this.loginTimeout = ModelStringUtil.parseNumberStringAsInt(loginTimeout);
	}

	private String getPersistantId() {
		File file = new File(getIdPersistFile());
		if (!file.exists()) {
			return null;
		}
		String data = StreamUtil.fileToString(file);
		if (data != null) {
			data = data.replace("\n", "").trim();
		}
		if (StringUtils.hasText(data)) {
			return data;
		} else {
			return null;
		}

	}

	private void setPersistantId(String id) throws IOException {
		FilePersistUtil.commitFile(getIdPersistFile(), id);
	}

	@Override
	public void process(final StroomZipRepository stroomZipRepository, Date fromDate) {
		Connection connection = null;
		try {
			connection = connect();

			final String originalId = getPersistantId();
			String id = originalId;

			if (id == null) {
				if (fromDate != null) {
					try {
						LOGGER.info(
								"process() - %s - Using fromDate query to obtain id - %s",
								feed, fromDateQuery);

						PreparedStatement idStatement = connection
								.prepareStatement(fromDateQuery);
						idStatement.setDate(1,
								new java.sql.Date(fromDate.getTime()));
						ResultSet resultSet = idStatement.executeQuery();
						if (resultSet.next()) {
							id = resultSet.getString(1);
						}
						resultSet.close();
						idStatement.close();
					} catch (SQLException sqlEx) {
						LOGGER.error(
								"process() - %s - Unable to get if with fromDateQuery - %s",
								feed, fromDateQuery, sqlEx);
					}

					if (id == null) {
						LOGGER.error(
								"process() - %s - Unable to get if with fromDateQuery - %s",
								feed, fromDateQuery);
						return;
					}
					LOGGER.info(
							"process() - %s - Now using id of %s with fromDateQuery - %s",
							feed, id, fromDateQuery);
					setPersistantId(id);
				}
			}

			if (id == null) {
				LOGGER.error(
						"process() - %s - No id set in %s.  You will need to this before we can continue. ",
						feed, getIdPersistFile());
				return;
			}
			
			final String startId = id;

			PreparedStatement statement = connection.prepareStatement(query);
			statement.setString(1, startId);
			ResultSet resultSet = statement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

			if (!resultSet.next()) {
				LOGGER.info("process() - %s - No new results from %s", feed, startId);

			} else {
				// Build a ZIP
				StroomZipOutputStream stroomZipOutputStream = null;
				PrintWriter printWriter = null;

				if (!isDebug()) {
					stroomZipOutputStream = stroomZipRepository
							.getStroomZipOutputStream();

					// Build the Data
					OutputStream dataStream = stroomZipOutputStream
							.addEntry(new StroomZipEntry(null, "extract",
									StroomZipFileType.Data));
					printWriter = new PrintWriter(dataStream);
				} else {
					printWriter = new PrintWriter(System.out);
				}

				int colCount = processHeader(resultSet, printWriter);
				long rowCount = 0;

				// Process the rows
				do {
					rowCount++;
					id = processRow(id, resultSet, resultSetMetaData,
							printWriter, colCount);

					if (maxRows != null && rowCount == maxRows) {
						break;
					}

				} while (resultSet.next());

				resultSet.close();
				statement.close();

				printWriter.close();

				// Don't bother if in debug mode
				if (!isDebug()) {
					HeaderMap headerMap = new HeaderMap();
					headerMap.put(HEADER_ARG_EXTRACT_START_ID, startId);
					headerMap.put(HEADER_ARG_EXTRACT_END_ID, id);
					populateHeaderMap(headerMap);

					OutputStream metaStream = stroomZipOutputStream
							.addEntry(new StroomZipEntry(null, "extract",
									StroomZipFileType.Meta));
					headerMap.write(metaStream, true);
				}

				LOGGER.info(
						"process() - %s - Exported %s rows. Id moved from %s to %s",
						feed, rowCount, originalId, id);

				// Don't bother if in debug mode
				if (!isDebug()) {
					setPersistantId(id);
					stroomZipOutputStream.close();
				}
			}

		} catch (Exception ex) {
			LOGGER.error("process() - %s - %s", feed, query, ex);
			LOGGER.error("process() - %s - %s - %s - %s - %s", idPersistFile, jdbcDriver, jdbcUrl, jdbcUser, headerMapPopulatorEntryList);
		} finally {
			disconnect(connection);
		}

	}

	@Override
	public void populateHeaderMap(HeaderMap headerMap) {
		headerMap.put(StroomAgentCollectConstants.HEADER_ARG_FEED, feed);
		headerMap.put(HEADER_ARG_JDBC_URL, jdbcUrl);
		headerMap.put(StroomAgentCollectConstants.HEADER_ARG_COLLECT_TIME,
				DateUtil.createNormalDateTimeString());

		for (HeaderMapPopulatorEntry headerMapPopulatorEntry : headerMapPopulatorEntryList) {
			headerMap.put(headerMapPopulatorEntry.getKey(),
					headerMapPopulatorEntry.getValue(headerMap));
		}
	}

	private Object getObject(ResultSet resultSet,
			ResultSetMetaData resultSetMetaData, int col) throws SQLException {
		int colType = resultSetMetaData.getColumnType(col);
		
		// Always return dates as time stamps so as to not lose precision 
		switch (colType) {
		case Types.DATE:
		case Types.TIME:
		case Types.TIMESTAMP:
			return resultSet.getTimestamp(col); 
		default:
			return resultSet.getObject(col);
		}

	}

	private String processRow(String id, ResultSet resultSet,
			ResultSetMetaData resultSetMetaData, PrintWriter printWriter,
			int count) throws SQLException {

		Object thisId = getObject(resultSet, resultSetMetaData, 1);

		if (checkSequence && StringUtils.hasText(id)) {
			if (thisId instanceof Number) {
				if (((Number) thisId).longValue() < Long.parseLong(id)) {
					throw new RuntimeException(
							"Error results are not in order for query");
				}
			} else {
				if (String.valueOf(thisId).compareTo(id) < 0) {
					throw new RuntimeException(
							"Error results are not in order for query");
				}
			}
		}

		id = String.valueOf(thisId);

		StringBuilder builder = new StringBuilder();

		for (int i = 1; i <= count; i++) {

			builder.append("\"");

			Object valueObj = getObject(resultSet, resultSetMetaData, i);
			String valueStr = null;
			if (isStroomDate() && valueObj instanceof Date) {
				valueStr = DateUtil
						.createNormalDateTimeString(((Date) valueObj).getTime());
			} else {
				valueStr = resultSet.getString(i);
			}

			if (valueStr != null) {
				for (int ci = 0; ci < valueStr.length(); ci++) {
					char c = valueStr.charAt(ci);
					if (c == '\"') {
						builder.append("\\");
					}
					builder.append(c);
				}
			}
			builder.append("\"");
			if (i < count) {
				builder.append(",");
			}
		}
		printWriter.println(builder.toString());
		return id;
	}

	private int processHeader(ResultSet resultSet, PrintWriter printWriter)
			throws SQLException {
		ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
		int count = resultSetMetaData.getColumnCount();

		StringBuilder builder = new StringBuilder();

		if (header) {
			for (int i = 1; i <= count; i++) {
				String columnName = resultSetMetaData.getColumnLabel(i);
				if (columnName.contains("\"") || columnName.contains(",")) {
					throw new RuntimeException(
							"Column headings must not contain ',' or '\"'. ["
									+ columnName + "]");
				}
				builder.append(columnName);
				if (i < count) {
					builder.append(",");
				}
			}
			printWriter.println(builder.toString());
		}
		return count;
	}

	private Connection connect() throws SQLException {
		Locale.setDefault(new Locale("en", "GB"));
		try {
			Class.forName(getJdbcDriver());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		if (loginTimeout != null) {
			DriverManager.setLoginTimeout(loginTimeout);
		}
		Connection connection = DriverManager.getConnection(getJdbcUrl(),
				getJdbcUser(), getJdbcPassword());
		LOGGER.info("connect() - Connected to %s", getJdbcUrl());
		return connection;
	}

	private void disconnect(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
				connection = null;
				LOGGER.info("disconnect() - Disconnected from %s", getJdbcUrl());
			} catch (SQLException e) {
				LOGGER.error("disconnect()", e);
			}
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + getFeed();
	}

}
