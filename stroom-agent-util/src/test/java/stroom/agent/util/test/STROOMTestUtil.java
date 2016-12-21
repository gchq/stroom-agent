package stroom.agent.util.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import stroom.agent.util.io.FileUtil;
import stroom.agent.util.thread.ThreadUtil;

/**
 * Test Utility.
 */
public class STROOMTestUtil {

	private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyyMMdd_HHmmss_SSS")
			.withZone(DateTimeZone.UTC);

	public static File createRootTestDir(final File tempDir) throws IOException {

		return tempDir;
	}

	public static File createTestDir(final File parentDir) throws IOException {
		if (!parentDir.isDirectory()) {
			throw new IOException("The parent directory '" + FileUtil.getCanonicalPath(parentDir) + "' does not exist");
		}

		File dir = null;
		for (int i = 0; i < 100; i++) {
			dir = new File(parentDir, FORMAT.print(System.currentTimeMillis()));
			if (dir.mkdir()) {
				break;
			} else {
				dir = null;
				ThreadUtil.sleep(100);
			}
		}

		if (dir == null) {
			throw new IOException("Unable to create unique test dir in: " + FileUtil.getCanonicalPath(parentDir));
		}

		return dir;
	}

	public static void destroyTestDir(final File testDir) {
		try {
			FileUtils.deleteDirectory(testDir);
		} catch (final IOException e) {
			// Ignore
		}
	}
}
