package stroom.agent.collect;

import stroom.agent.util.io.StreamUtil;
import stroom.agent.util.zip.StroomZipEntry;
import stroom.agent.util.zip.StroomZipFileType;
import stroom.agent.util.zip.StroomZipOutputStream;
import stroom.agent.util.zip.HeaderMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import com.jcraft.jsch.SftpException;

public class SFTPGetHandlerCommonsCompressZIP implements SFTPGetHandler {

	private boolean gzip = false;

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


		ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(
				sftpTransfer.getRemoteStream(remoteFile));
		ZipArchiveEntry zipEntry = null;
		while ((zipEntry = zipInputStream.getNextZipEntry()) != null) {

			if (!zipEntry.isDirectory()) {

				if (filePattern == null
						|| filePattern.matcher(zipEntry.getName()).matches()) {

					HeaderMap headerMap = new HeaderMap();
					headerMap.put(
							StroomAgentCollectConstants.HEADER_ARG_REMOTE_FILE,
							remoteFile.getPath()+ "@" + zipEntry.getName());
					headerMapPopulator.populateHeaderMap(headerMap);

					OutputStream metaStream = stroomZipOutputStream
							.addEntry(new StroomZipEntry(null, "extract" + count,
									StroomZipFileType.Meta));
					headerMap.write(metaStream, true);

					OutputStream dataStream = stroomZipOutputStream
							.addEntry(new StroomZipEntry(null, "extract" + count,
									StroomZipFileType.Data));

					InputStream dataInStream = zipInputStream;
					if (isGzip()) {
						dataInStream = new GzipCompressorInputStream(
								dataInStream, true);
					}
					StreamUtil.streamToStream(dataInStream, dataStream, false);
					dataStream.close();

					count++;
				}
			}
		}

		return count;

	}

	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}

	public boolean isGzip() {
		return gzip;
	}
}
