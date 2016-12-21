package stroom.agent.dispatch;

import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.shared.ModelStringUtil;
import stroom.agent.util.shared.Monitor;
import stroom.agent.util.thread.ThreadLocalBuffer;
import stroom.agent.util.zip.StroomHeaderArguments;
import stroom.agent.util.zip.StroomZipRepository;
import stroom.agent.util.zip.StroomZipRepositorySimpleExecutorProcessor;
import stroom.agent.util.zip.HeaderMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.springframework.util.StringUtils;

public class SimpleDispatcher extends StroomZipRepositorySimpleExecutorProcessor implements Dispatcher {

	public SimpleDispatcher(Monitor monitor) {
		super(monitor);
	}

	private final static StroomLogger LOGGER = StroomLogger.getLogger(SimpleDispatcher.class); 
	
	private final static ThreadLocalBuffer THREAD_LOCAL_BUFFER = new ThreadLocalBuffer(); 
	
	private String forwardUrl;
	private boolean readOnly = false;
	private Integer forwardTimeoutMs = null;
	private Integer failLimit = null;
	private Integer forwardChunkSize = null;
	
	/**
	 * Just send them one by one
	 */

	@Override
	public void processFeedFiles(StroomZipRepository stroomZipRepository,
			String feed, List<File> fileList) {
		int notOkResponse = 0;
		if (StringUtils.hasText(getForwardUrl())) {
			for (File file : fileList) {
				if (!processFeedFile(stroomZipRepository, feed, file)) {
					notOkResponse++;
				}
				if (failLimit != null && notOkResponse>=failLimit) {
					LOGGER.error("processFeedFiles() - Will Quit as we hit our fail limit %s ", failLimit);
					return;
				}
			}
		}
	}
	
	/**
	 * Send a file and delete it
	 */
	private boolean processFeedFile(StroomZipRepository stroomZipRepository,
			String feed, File file) {
		boolean okResponse = true;
		try {
			HeaderMap headerMap = new HeaderMap();
			headerMap.put(StroomHeaderArguments.FEED, feed);
			headerMap.put(StroomHeaderArguments.COMPRESSION,
					StroomHeaderArguments.COMPRESSION_ZIP);
	
			URL url = new URL(forwardUrl);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
	
	
			if (connection instanceof HttpsURLConnection) {
				((HttpsURLConnection) connection)
						.setHostnameVerifier(new HostnameVerifier() {
							public boolean verify(final String arg0,
									final SSLSession arg1) {
								System.out
										.println("HostnameVerifier - " + arg0);
								return true;
							}
						});
			}
			
			if (forwardTimeoutMs != null) {
				connection.setConnectTimeout(forwardTimeoutMs);
				// Don't set a read time out else big files will fail
				connection.setReadTimeout(0);
			}
			
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/audit");
			connection.setRequestProperty("user-agent", "StroomAgent/1.0 Java/"+System.getProperty("java.version"));
			connection.setDoOutput(true);
			connection.setDoInput(true);
			
			// Also add all our command options
			for (String arg : headerMap.keySet()) {
				connection.addRequestProperty(arg, headerMap.get(arg));
			}
			
			if (forwardChunkSize != null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("processFeedFile() - setting ChunkedStreamingMode = %s", forwardChunkSize);
				}
				connection.setChunkedStreamingMode(forwardChunkSize);
			}

	
			connection.connect();
			OutputStream out = connection.getOutputStream();
	
			FileInputStream fis = new FileInputStream(file);
	
			// Write the output
			byte[] buffer = getReadBuffer();
			int readSize;
			while ((readSize = fis.read(buffer)) != -1) {
				out.write(buffer, 0, readSize);
			}
	
			out.flush();
			out.close();
			fis.close();
	
			int response = connection.getResponseCode();
	
			
			String msg = connection.getResponseMessage();
	
			
			if (response != 200) {
				LOGGER.error("processFeedFile() - Failed to send file %s %s return code was %s %s", file, feed, response, msg);
			} else {
				LOGGER.info("processFeedFile() - Sent file %s %s return code was %s", file, feed, response);
				if (!readOnly) {
					file.delete();
				}
			}
		} catch (Exception e) {
			okResponse = false;
			LOGGER.error("processFeedFile() - Failed to send file %s %s (%s)", file, feed, e.getMessage());
			LOGGER.debug("processFeedFile() - Failed to send file %s %s", file, feed, e);
		}
		return okResponse;
				
	}

	public String getForwardUrl() {
		return forwardUrl;
	}

	public void setForwardUrl(String forwardUrl) {
		this.forwardUrl = forwardUrl;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	public void setForwardTimeoutMs(String forwardTimeoutMs) {
		this.forwardTimeoutMs = ModelStringUtil.parseNumberStringAsInt(forwardTimeoutMs);
	}
	
	public void setFailLimit(String failLimit) {
		this.failLimit = ModelStringUtil.parseNumberStringAsInt(failLimit);
	}

	public void setForwardChunkSize(String forwardChunkSize) {
		this.forwardChunkSize = ModelStringUtil.parseNumberStringAsInt(forwardChunkSize);
	}
	
	
	@Override
	public boolean isReadOnly() {
		return readOnly;
	}
	
	@Override
	public byte[] getReadBuffer() {
		return THREAD_LOCAL_BUFFER.getBuffer();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + getForwardUrl();
	}

}
