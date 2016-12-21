package stroom.agent.collect;

import stroom.agent.util.zip.StroomZipOutputStream;

import java.io.IOException;

import com.jcraft.jsch.SftpException;

public interface SFTPGetHandler {
	int getFile(SFTPTransfer sftpTransfer, StroomZipOutputStream stroomZipOutputStream, HeaderMapPopulator headerMapPopulator,
			SFTPFileDetails remoteFile, int count) throws IOException, SftpException;
}
