package stroom.agent.collect;

import stroom.agent.util.io.FileUtil;
import stroom.agent.util.io.StreamUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public final class STROOMAgentTestFileUtil {
	private static File testDataDir;
	private static File testOutputDir;

	private STROOMAgentTestFileUtil() {
		// Utility class.
	}

	public static File getTestDataDir() {
		if (testDataDir == null) {
			testDataDir = new File("src/test/resources/test-data");
			if (!testDataDir.isDirectory()) {
				throw new RuntimeException("Test data directory not found: "
						+ testDataDir.getAbsolutePath());
			}
		}

		return testDataDir;
	}

	public static File getTestOutputDir() {
		if (testOutputDir == null) {
			testOutputDir = new File("target/stroom-agent/test-output");
			FileUtil.mkdirs(testOutputDir);
			
			if (!testOutputDir.isDirectory()) {
				throw new RuntimeException("Test output directory not found: "
						+ testOutputDir.getAbsolutePath());
			}
		}

		return testOutputDir;
	}
	
	public static File getFile(final String path) {
		final File file = new File(getTestDataDir(), path);
		if (!file.isFile()) {
			throw new RuntimeException("File not found: "
					+ file.getAbsolutePath());
		}
		return file;
	}

	public static InputStream getInputStream(final String path) {
		final File file = new File(getTestDataDir(), path);
		if (!file.isFile()) {
			throw new RuntimeException("File not found: "
					+ file.getAbsolutePath());
		}
		try {
			return new BufferedInputStream(new FileInputStream(file));
		} catch (final FileNotFoundException e) {
			throw new RuntimeException("File not found: "
					+ file.getAbsolutePath());
		}
	}

	public static String getString(final String path) {
		final InputStream is = getInputStream(path);
		return StreamUtil.streamToString(is);
	}
}
