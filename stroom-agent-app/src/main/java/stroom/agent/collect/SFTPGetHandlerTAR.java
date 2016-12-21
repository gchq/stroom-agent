package stroom.agent.collect;

import stroom.agent.util.io.StreamUtil;
import stroom.agent.util.zip.StroomZipEntry;
import stroom.agent.util.zip.StroomZipFileType;
import stroom.agent.util.zip.StroomZipOutputStream;
import stroom.agent.util.zip.HeaderMap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.jcraft.jsch.SftpException;

public class SFTPGetHandlerTAR implements SFTPGetHandler {

	public Pattern filePattern = null;

	public void setFile(String tarFile) {
		if (tarFile == null) {
			filePattern = null;
		} else {
			filePattern = Pattern.compile(tarFile);
		}
	}

	public int getFile(SFTPTransfer sftpTransfer, StroomZipOutputStream stroomZipOutputStream, HeaderMapPopulator headerMapPopulator, SFTPFileDetails remoteFile,
			int count) throws IOException, SftpException {


		TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
				sftpTransfer.getRemoteStream(remoteFile));

		ArchiveEntry archiveEntry = null;
		while ((archiveEntry = tarArchiveInputStream.getNextEntry()) != null) {

			if (!archiveEntry.isDirectory()) {
				String name = archiveEntry.getName();

				if (filePattern == null || filePattern.matcher(name).matches()) {

					HeaderMap headerMap = new HeaderMap();
					headerMap.put(
							StroomAgentCollectConstants.HEADER_ARG_REMOTE_FILE,
							remoteFile.getPath() + "@" + archiveEntry.getName());
					headerMapPopulator.populateHeaderMap(headerMap);

					OutputStream metaStream = stroomZipOutputStream
							.addEntry(new StroomZipEntry(null, "extract" + count,
									StroomZipFileType.Meta));
					headerMap.write(metaStream, true);

					OutputStream dataStream = stroomZipOutputStream
							.addEntry(new StroomZipEntry(null, "extract" + count,
									StroomZipFileType.Data));

					StreamUtil.streamToStream(tarArchiveInputStream,
							dataStream, false);
					dataStream.close();
					count++;
				}
			}
		}

		tarArchiveInputStream.close();

		return count;

	}
}
