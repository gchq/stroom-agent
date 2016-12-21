package stroom.agent.util.test;

/**
 * Some handy test methods.
 *
 */
public abstract class FileSystemTestUtil {


	public static final char SEPERATOR_CHAR = '/';

	public FileSystemTestUtil() {
		// Utility
	}

	private static final long TEST_PREFIX = System.currentTimeMillis();
	private static long testSuffix = 0;

	/**
	 * @return a unique string for testing
	 */
	public static synchronized String getUniqueTestString() {
		testSuffix++;
		return TEST_PREFIX + "_" + testSuffix;
	}
}
