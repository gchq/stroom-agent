package stroom.agent.util.io;

import java.io.IOException;
import java.io.InputStream;

public class WrappedInputStream extends InputStream {
	private final InputStream inputStream;

	public WrappedInputStream(final InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return inputStream.available();
	}

	/**
	 * @throws IOException
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		inputStream.close();
	}

	/**
	 * @param readlimit
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public synchronized void mark(int readlimit) {
		inputStream.mark(readlimit);
	}

	/**
	 * @return
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return inputStream.markSupported();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		return inputStream.read();
	}

	/**
	 * @param b
	 * @param off
	 * @param len
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return inputStream.read(b, off, len);
	}

	/**
	 * @param b
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return inputStream.read(b);
	}

	/**
	 * @throws IOException
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
	}

	/**
	 * @param n
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		return inputStream.skip(n);
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		return inputStream.equals(obj);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		return inputStream.hashCode();
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return inputStream.toString();
	}
}
