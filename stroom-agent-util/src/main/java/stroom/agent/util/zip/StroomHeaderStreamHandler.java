package stroom.agent.util.zip;

import java.io.IOException;

public interface StroomHeaderStreamHandler {
	public void handleHeader(HeaderMap headerMap) throws IOException;

}
