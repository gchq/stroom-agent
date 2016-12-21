package stroom.agent.util.zip;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface StroomHeaderArguments {
	public final static String GUID = "GUID";
	public final static String COMPRESSION = "Compression";
	public final static String COMPRESSION_ZIP = "ZIP";
	public final static String COMPRESSION_GZIP = "GZIP";
	public final static String COMPRESSION_NONE = "NONE";

	public final static Set<String> VALID_COMPRESSION_SET = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(
					COMPRESSION_GZIP, COMPRESSION_ZIP, COMPRESSION_NONE)));

	public final static String CONTENT_LENGTH = "content-length";
	public final static String USER_AGENT = "user-agent";


	public final static String REMOTE_ADDRESS = "RemoteAddress";
	public final static String REMOTE_HOST = "RemoteHost";
	public final static String RECEIVED_TIME = "ReceivedTime";
	public final static String RECEIVED_PATH = "ReceivedPath";
	public final static String EFFECTIVE_TIME = "EffectiveTime";
	public final static String REMOTE_DN = "RemoteDN";
	public final static String REMOTE_CERT_EXPIRY = "RemoteCertExpiry";
	public final static String REMOTE_FILE = "RemoteFile";


	public final static String STREAM_SIZE = "StreamSize";

	public final static String STROOM_STATUS = "Stroom-Status";

	public final static String FEED = "Feed";

	public final static Set<String> HEADER_CLONE_EXCLUDE_SET = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList("accept",
					"connection", "content-length", "transfer-encoding",
					"expect", COMPRESSION)));

}
