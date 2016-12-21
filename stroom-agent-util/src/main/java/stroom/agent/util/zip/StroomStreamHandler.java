package stroom.agent.util.zip;

import java.io.IOException;

public interface StroomStreamHandler {
	public void handleEntryStart(StroomZipEntry stroomZipEntry) throws IOException;

	public void handleEntryData(byte[] data, int off, int len)
			throws IOException;

	public void handleEntryEnd() throws IOException;

}
