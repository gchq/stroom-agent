package stroom.agent.collect;

import stroom.agent.util.date.DateUtil;
import stroom.agent.util.io.CloseableUtil;
import stroom.agent.util.io.IgnoreCloseInputStream;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.zip.StroomHeaderArguments;
import stroom.agent.util.zip.StroomStatusCode;
import stroom.agent.util.zip.StroomStreamException;
import stroom.agent.util.zip.StroomZipEntry;
import stroom.agent.util.zip.StroomZipFileType;
import stroom.agent.util.zip.StroomZipOutputStream;
import stroom.agent.util.zip.StroomZipRepository;
import stroom.agent.util.zip.HeaderMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

import com.jcraft.jsch.SftpException;

public class HeadlessStroomFileProcessor implements ContentHandler {


	private final static StroomLogger LOGGER = StroomLogger
			.getLogger(HeadlessStroomSFTPCollector.class);
	
	private static final SAXParserFactory PARSER_FACTORY;
	static {
		PARSER_FACTORY = SAXParserFactory.newInstance();
		PARSER_FACTORY.setNamespaceAware(true);
	}
	private static final String XML = "xml";
	private static final String UTF_8 = "UTF-8";
	private static final String YES = "yes";

	public final static String RESULT_FILE = "results";

	public final static String METADATA_TAG = "MetaData";
	public final static String EVENT_TAG = "Event";
	public final static String ENTRY_TAG = "Entry"; 
	public final static String KEY_ATTRIBUTE = "Key"; 
	public final static String VALUE_ATTRIBUTE = "Value"; 

	private Transformer serializer;
	private ContentHandler xmlWriter;
	private StroomZipRepository stroomZipRepository;
	private Map<String, StroomZipOutputStream> feedToStroomZipOutputStreamMap; 
	private Map<String, OutputStream> feedToOutputStreamMap; 

	private int depth = 0;
	private int fileCount = 0;
	private int filePartCount = 0;
	private int errorCount = 0;

	private String startElementUri;
	private String startElementLocalName;
	private String startElementQName;
	private Attributes startElementAttributes;
	private Map<String, String> startPrefixMapping = new HashMap<String, String>();
	private HeaderMap metaData = null;
	private String level2Element = null;
	private boolean doneEndDocument;
	private Set<String> feedSet = new HashSet<String>();
	private String[] feedMatch;
	private String[] feedReplace;
	private String processFilePath;

	
	public String getFeed() {
		return feedSet.toString();
	}
	
	private void init() {
		startElementUri = null;
		startElementLocalName = null;
		startElementQName = null;
		startElementAttributes = null;
		startPrefixMapping = new HashMap<String, String>();
		metaData = null;
		doneEndDocument = false;
		serializer = null;
		xmlWriter = null;
		feedToStroomZipOutputStreamMap = new HashMap<String, StroomZipOutputStream>();
		feedToOutputStreamMap = new HashMap<String, OutputStream>();
	}
	
	public void setStroomZipRepository(StroomZipRepository stroomZipRepository) {
		this.stroomZipRepository = stroomZipRepository;
	}

	public void processFile(final SFTPTransfer sftpTransfer, final StroomZipRepository stroomZipRepository, SFTPFileDetails fileDetails, boolean delete, final String renameSuffix) throws IOException {
		setStroomZipRepository(stroomZipRepository);
		processFilePath = fileDetails.getPath();
		init();
		InputStream stream = null;
		try {
			// Open the stream and read the tar
			stream = getRemoteStream(sftpTransfer, fileDetails);
			TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
					stream);
	
			ArchiveEntry archiveEntry = null;
			while ((archiveEntry = tarArchiveInputStream.getNextEntry()) != null) {
	
				if (!archiveEntry.isDirectory()) {
					String name = archiveEntry.getName();
	
					if (RESULT_FILE.equalsIgnoreCase(name)) {
						LOGGER.debug("processFile() - Processing %s", archiveEntry.getName());
						processStream(new IgnoreCloseInputStream(tarArchiveInputStream));
					} else {
						LOGGER.debug("processFile() - Ingore %s", archiveEntry.getName());
					}
				} else {
					LOGGER.debug("processFile() - Ingore %s", archiveEntry.getName());
				}
			}
			if (delete) {
				deleteRemoteFile(sftpTransfer, fileDetails);
			} else {
				if (StringUtils.hasText(renameSuffix)) {
					renameRemoteFile(sftpTransfer, fileDetails, renameSuffix);
				}
			}
			
			closeStroomZipOutputStreams();
			
			feedToStroomZipOutputStreamMap = null;
		} catch (SftpException sftpException) {
			throw new IOException(sftpException);
		}
		finally {
			// Any exceptions delete the locked files
			deleteStroomZipOutputStreams();		
			CloseableUtil.close(stream);
		}
	}

	private void deleteStroomZipOutputStreams() throws IOException {
		if (feedToStroomZipOutputStreamMap != null) {
			Iterator<StroomZipOutputStream> stroomZipOutputStreamItr = feedToStroomZipOutputStreamMap.values().iterator();
			while (stroomZipOutputStreamItr.hasNext()) {
				StroomZipOutputStream stroomZipOutputStream = stroomZipOutputStreamItr.next();
				stroomZipOutputStream.closeDelete();
				stroomZipOutputStreamItr.remove();
			}
			feedToStroomZipOutputStreamMap = null;
		}
	}

	private void closeStroomZipOutputStreams() throws IOException {
		Iterator<StroomZipOutputStream> stroomZipOutputStreamItr = feedToStroomZipOutputStreamMap.values().iterator();
		while (stroomZipOutputStreamItr.hasNext()) {
			StroomZipOutputStream stroomZipOutputStream = stroomZipOutputStreamItr.next();
			stroomZipOutputStream.close();
			stroomZipOutputStreamItr.remove();
		}
	}

	public InputStream getRemoteStream(final SFTPTransfer sftpTransfer,
			SFTPFileDetails fileDetails) throws SftpException {
		return sftpTransfer.getRemoteStream(fileDetails);
	}

	public void deleteRemoteFile(final SFTPTransfer sftpTransfer,
			SFTPFileDetails fileDetails) throws SftpException {
		sftpTransfer.delete(fileDetails);
	}

	public void renameRemoteFile(final SFTPTransfer sftpTransfer,
			SFTPFileDetails fileDetails, String suffix) throws SftpException {
		
		sftpTransfer.rename(fileDetails, suffix);
	}

	public void processStream(final InputStream stream) throws IOException {
		try {
			// Make an XML reader that produces SAX events.
			SAXParser parser = null;
			try {
				parser = PARSER_FACTORY.newSAXParser();
			} catch (final ParserConfigurationException e) {
				throw new IOException(e);
			}
			XMLReader xmlReader = parser.getXMLReader();
			xmlReader.setContentHandler(this);
			xmlReader.parse(new InputSource(stream));

		} catch (SAXException sax) {
			throw new IOException(sax);
		} 
	}

	public void openWriter()
			throws SAXException {
		LOGGER.debug("openWriter()");
		
		if (xmlWriter != null) {
			throw new SAXException("Writer still open");
		} else {

			if (metaData == null) {
				throw new SAXException("Attempt to start writing XML without processing meta data to indicate which feed the data is for " + processFilePath);
			}
			
			
			String feed = metaData.get(StroomHeaderArguments.FEED);
			
			if (feed == null) {
				throw new StroomStreamException(StroomStatusCode.FEED_MUST_BE_SPECIFIED);
			}

			filePartCount ++;
			try {
				
				
				StroomZipOutputStream stroomZipOutputStream = feedToStroomZipOutputStreamMap.get(feed);
				if (stroomZipOutputStream == null) {
					stroomZipOutputStream = stroomZipRepository.getStroomZipOutputStream();
					LOGGER.debug("openWriter() - %s - Opened %s for feed %s", processFilePath, stroomZipOutputStream.getFinalFile(),feed);
					feedToStroomZipOutputStreamMap.put(feed, stroomZipOutputStream);
					fileCount ++;
				}
				// Close off last entry?
				OutputStream outputStream = feedToOutputStreamMap.get(feed);
				if (outputStream != null) {
					outputStream.close();
				}
				
				
				
				
				final SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory
						.newInstance();
				final TransformerHandler th = stf.newTransformerHandler();
				serializer = th.getTransformer();
				serializer.setOutputProperty(OutputKeys.METHOD, XML);
				serializer.setOutputProperty(OutputKeys.ENCODING, UTF_8);
				serializer.setOutputProperty(OutputKeys.INDENT, YES);
				metaData.write(stroomZipOutputStream.addEntry(new StroomZipEntry(null, String.valueOf(filePartCount), StroomZipFileType.Meta)), true);
				OutputStream dataStream = stroomZipOutputStream.addEntry(new StroomZipEntry(null, String.valueOf(filePartCount), StroomZipFileType.Data));
				feedToOutputStreamMap.put(feed, dataStream);
				th.setResult(new StreamResult(dataStream));
				xmlWriter = th;
				
				xmlWriter.startDocument();
				for (Map.Entry<String, String> prefix : startPrefixMapping.entrySet()) {
					xmlWriter.startPrefixMapping(prefix.getKey(), prefix.getValue());
				}
				xmlWriter.startElement(startElementUri, startElementLocalName, startElementQName, startElementAttributes);

			} catch (TransformerConfigurationException sax) {
				throw new SAXException(sax);
			} catch (IOException ioEx) {
				throw new SAXException(ioEx);
			}
		}
	}
	public void closeWriter() throws SAXException {
		if (xmlWriter != null) {
			if (!doneEndDocument) {
				xmlWriter.endElement(startElementUri, startElementLocalName, startElementQName);
				xmlWriter.endDocument();
			}
			xmlWriter = null;
		}
	}
	

	@Override
	public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.characters(arg0, arg1, arg2);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.endDocument();
		}
		doneEndDocument = true;
		closeWriter();
	}

	@Override
	public void endPrefixMapping(String arg0) throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.endPrefixMapping(arg0);
		}

	}

	@Override
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
			throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.ignorableWhitespace(arg0, arg1, arg2);
		}
	}

	@Override
	public void processingInstruction(String arg0, String arg1)
			throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.processingInstruction(arg0, arg1);
		}
	}

	@Override
	public void setDocumentLocator(Locator arg0) {
		if (xmlWriter != null) {
			xmlWriter.setDocumentLocator(arg0);
		}
	}

	@Override
	public void skippedEntity(String arg0) throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.skippedEntity(arg0);
		}
	}

	@Override
	public void startDocument() throws SAXException {
		LOGGER.debug("startDocument()");
		if (xmlWriter != null) {
			xmlWriter.startDocument();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		depth++;
		if (depth <= 2) {
			LOGGER.debug("startElement() %s - %s %s", processFilePath, localName, qName);
		} else {
			LOGGER.trace("startElement() %s - %s %s", processFilePath, localName, qName);
		}

		// Root Element ?
		if (startElementUri == null) {
			startElementUri = uri;
			startElementLocalName = localName;
			startElementQName = qName;
			startElementAttributes = new AttributesImpl(atts);
		}
		if (depth == 2) {
			level2Element = localName;
			if (METADATA_TAG.equals(localName)) {
				metaData = new HeaderMap();
				metaData.put(StroomAgentCollectConstants.HEADER_ARG_COLLECT_TIME, DateUtil.createNormalDateTimeString(System.currentTimeMillis()));
				closeWriter();
			} else if (EVENT_TAG.equals(localName)) {
				if (xmlWriter == null) {
					openWriter();
				}
			} else {
				StringBuilder builder = new StringBuilder();
				builder.append(processFilePath);
				builder.append(" - ");
				if (metaData != null) {
					String feed = metaData.get(StroomHeaderArguments.FEED);
					builder.append(feed);
				} else {
					builder.append("null");
				}
				builder.append(" - Content not matching 'Event' or 'MetaData' - '");
				builder.append(level2Element);
				builder.append("' - <");
				builder.append(localName);
				builder.append(" ");
				if (atts != null) {
					for (int i=0; i<atts.getLength(); i++) {
						builder.append(" ");
						builder.append(atts.getLocalName(i));
						builder.append("=\"");
						builder.append(atts.getValue(i));
						builder.append("\"");
					}
				}
				builder.append("/>");
				errorCount++;
				
				LOGGER.error(builder.toString());
				
			}
		}
		if (METADATA_TAG.equals(level2Element) && ENTRY_TAG.equals(localName)) {
			String key = atts.getValue(KEY_ATTRIBUTE);
			String value = atts.getValue(VALUE_ATTRIBUTE);
			if (key != null) {
				metaData.put(key, value);
			}
		}
		if (xmlWriter != null) {
			xmlWriter.startElement(uri, localName, qName, atts);
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (depth <= 2) {
			LOGGER.debug("endElement() %s - %s %s", processFilePath, localName, qName);
		} else {
			LOGGER.trace("endElement() %s - %s %s", processFilePath, localName, qName);
		}
		// Just Finished Event or Meta Data?
		if (depth == 2) {
			level2Element = null;
			
			if (METADATA_TAG.equals(localName)) {
				String feed = metaData.get(StroomHeaderArguments.FEED);
				if (feedMatch != null && feedReplace != null) {
					feed = feedReplace(feed);
					metaData.put(StroomHeaderArguments.FEED, feed);
				}
				if (feed != null) {
					feedSet.add(feed);
				}
			}
		}

		if (xmlWriter != null) {
			xmlWriter.endElement(uri, localName, qName);
		}
		depth--;
	}

	public String feedReplace(final String feed) throws SAXException  {
		if (feedMatch != null && feedReplace != null) {
			String rtnVal = null;
			for (int i=0; i<feedMatch.length; i++) {
				if (feed.contains(feedMatch[i])) {
					if (i<feedReplace.length) {
						rtnVal = feed.replace(feedMatch[i], feedReplace[i]); 
					} else {
						// Match but no replacement
						rtnVal = feed;
					}
					break;
				}
			}
			
			if (rtnVal == null) {
				// No Match 
				throw new SAXException("Feed " + feed + " did not match any of " + Arrays.toString(feedMatch) + " " + processFilePath);
			}

			LOGGER.debug("feedReplace() - %s - %s -> %s", processFilePath, feed, rtnVal);
			return rtnVal;
		
		}
		return feed;
	}
	
	
	public int getFileCount() {
		return fileCount;
	}
	public int getFilePartCount() {
		return filePartCount;
	}
	public int getErrorCount() {
		return errorCount;
	}

	@Override
	public void startPrefixMapping(String arg0, String arg1)
			throws SAXException {
		if (xmlWriter != null) {
			xmlWriter.startPrefixMapping(arg0, arg1);
		} else {
			startPrefixMapping.put(arg0, arg1);
		}
	}
	public void setFeedMatch(String[] feedMatch) {
		this.feedMatch = feedMatch;
	}
	public void setFeedReplace(String[] feedReplace) {
		this.feedReplace = feedReplace;
	}

}
