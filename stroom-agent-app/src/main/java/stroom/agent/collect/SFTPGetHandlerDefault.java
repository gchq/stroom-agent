package stroom.agent.collect;

import stroom.agent.util.io.StreamUtil;
import stroom.agent.util.zip.StroomZipEntry;
import stroom.agent.util.zip.StroomZipFileType;
import stroom.agent.util.zip.StroomZipOutputStream;
import stroom.agent.util.zip.HeaderMap;

import java.io.IOException;
import java.io.OutputStream;

import com.jcraft.jsch.SftpException;

public class SFTPGetHandlerDefault implements SFTPGetHandler {
	
	public int getFile(SFTPTransfer sftpTransfer, StroomZipOutputStream stroomZipOutputStream, HeaderMapPopulator headerMapPopulator, SFTPFileDetails remoteFile,
			int count) throws IOException, SftpException {

		HeaderMap headerMap = new HeaderMap();
		headerMap.put(StroomAgentCollectConstants.HEADER_ARG_REMOTE_FILE,
				remoteFile.getPath());
		headerMapPopulator.populateHeaderMap(headerMap);

		OutputStream metaStream = stroomZipOutputStream.addEntry(new StroomZipEntry(
				null, "extract" + count, StroomZipFileType.Meta));
		headerMap.write(metaStream, true);

		OutputStream dataStream = stroomZipOutputStream.addEntry(new StroomZipEntry(
				null, "extract" + count, StroomZipFileType.Data));

		StreamUtil.streamToStream(sftpTransfer.getRemoteStream(remoteFile), dataStream);
				
		return count + 1;

	}

}
