package stroom.agent.util.io;


import java.io.IOException;
import java.io.InputStream;

public class IgnoreCloseInputStream extends WrappedInputStream {
	public IgnoreCloseInputStream(final InputStream inputStream) {
		super(inputStream);
	}

	public static IgnoreCloseInputStream wrap(final InputStream inputStream) {
		if (inputStream == null) {
			return null;
		}

		return new IgnoreCloseInputStream(inputStream);
	}

	@Override
	public void close() throws IOException {
		// Ignore calls to close the stream.
	}
}
