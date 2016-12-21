package stroom.agent.util.io;

import java.io.File;
import java.io.IOException;

import stroom.agent.util.config.StroomProperties;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.thread.ThreadUtil;

public final class FileUtil {
	private static final StroomLogger LOGGER = StroomLogger.getLogger(FileUtil.class);

	private FileUtil() {
		// Utility.
	}

	/**
	 * JVM wide temp dir
	 */
	private volatile static File tempDir = null;
	//
	// /**
	// * @return the java IO temp dir.
	// */
	// public static File createTempDir(final String path) {
	// if (tempDir == null) {
	// String path = System.getProperty(STROOM_TEMP);
	// if (path == null) {
	// path = System.getenv(STROOM_TEMP);
	// }
	// if (path == null) {
	// path = System.getProperty("java.io.tmpdir");
	// if (path != null) {
	// path = path + "/stroom";
	// }
	// }
	// // Finally make sure the system properties are set so that we don't
	// // have
	// // to keep checking environment variables as well.
	// System.setProperty(STROOM_TEMP, path);
	//
	// if (path != null) {
	// tempDir = new File(path);
	// }
	// }
	// return tempDir;
	// }

	public static File getInitialTempDir() {
		File dir = null;

		try {
			final String path = StroomProperties.getProperty(StroomProperties.STROOM_TEMP);

			/* Ensure we have a path. */
			if (path == null) {
				throw new IOException("No temp path is specified");
			}

			/* Ensure the temp directory exists. */
			dir = new File(path);
			if (!dir.isDirectory()) {
				throw new IOException("Temp directory '" + path + "' does not exist");
			}

		} catch (final IOException e) {
			LOGGER.fatal(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}

		return dir;
	}

	public static File getTempDir() {
		if (tempDir == null) {
			synchronized (FileUtil.class) {
				if (tempDir == null) {
					tempDir = getInitialTempDir();
				}
			}
		}

		return tempDir;
	}

	public static void useDevTempDir() {
		try {
			final File tempDir = getTempDir();

			final File devDir = new File(tempDir, "dev");
			mkdirs(devDir);

			final String path = devDir.getCanonicalPath();

			/* Redirect the temp dir for dev. */
			StroomProperties.setOverrideProperty(StroomProperties.STROOM_TEMP, path, "dev");
			/*
			 * Also set the temp dir as a system property as EclipseDevMode
			 * starts a new JVM and will forget this property otherwise.
			 */
			System.setProperty(StroomProperties.STROOM_TEMP, path);

			LOGGER.info("Using temp dir '" + path + "'");

			forgetTempDir();

		} catch (final IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * @param dir
	 *            system wide DIR
	 */
	public static void forgetTempDir() throws IOException {
		synchronized (FileUtil.class) {
			tempDir = null;
		}
	}

	public final static int MKDIR_RETRY_COUNT = 2;
	public final static int MKDIR_RETRY_SLEEP_MS = 100;

	public static void createNewFile(final File file) throws IOException {
		if (!file.createNewFile()) {
			throw new FileUtilException("Unable to create new file: " + file.getAbsolutePath());
		}
	}

	public static void deleteFile(final File file) {
		if (file.exists()) {
			if (!file.isFile()) {
				throw new FileUtilException("Path is directory not file \"" + file.getAbsolutePath() + "\"");
			}

			if (!file.delete()) {
				throw new FileUtilException("Unable to delete \"" + file.getAbsolutePath() + "\"");
			}
		}
	}

	public static void deleteDir(final File file) {
		if (file.exists()) {
			if (!file.delete()) {
				throw new FileUtilException("Unable to delete \"" + file.getAbsolutePath() + "\"");
			}
		}
	}

	public static void forceDelete(final File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				for (final File f : file.listFiles()) {
					forceDelete(f);
				}
			}

			try {
				if (!file.delete()) {
					file.deleteOnExit();
				}
			} catch (final Exception e) {
			}
		}
	}

	public static void mkdirs(final File dir) {
		if (!dir.isDirectory()) {
			if (!doMkdirs(null, dir, MKDIR_RETRY_COUNT)) {
				throw new FileUtilException("Unable to make directory: " + dir.getAbsolutePath());
			}

			if (!dir.isDirectory()) {
				throw new FileUtilException("Directory not found: " + dir.getAbsolutePath());
			}
		}
	}

	public static boolean doMkdirs(final File superDir, final File dir, int retry) {
		// Make sure the parent exists first
		final File parentDir = dir.getParentFile();
		if (parentDir != null && !parentDir.isDirectory()) {
			if (superDir != null && superDir.equals(parentDir)) {
				// Unable to make parent as it is the super dir
				return false;

			}
			if (!doMkdirs(superDir, parentDir, retry)) {
				// Unable to make parent :(
				return false;
			}
		}
		// No Make us
		if (!dir.isDirectory()) {
			// * CONCURRENT PROBLEM AREA *
			if (!dir.mkdir()) {
				// Someone could have made it in the * CONCURRENT PROBLEM AREA *
				if (!dir.isDirectory()) {
					if (retry > 0) {
						retry = retry - 1;
						LOGGER.warn("doMkdirs() - Sleep and Retry due to unable to create " + dir.getAbsolutePath());
						ThreadUtil.sleep(MKDIR_RETRY_SLEEP_MS);
						return doMkdirs(superDir, dir, retry);
					} else {
						return false;
					}
				}
			}
		}
		return true;

	}

	public static void rename(final File src, final File dest) {
		if (!src.renameTo(dest)) {
			throw new FileUtilException(
					"Unable to rename file \"" + src.getAbsolutePath() + "\" to \"" + dest.getAbsolutePath() + "\"");
		}
	}

	public static void setLastModified(final File file, final long time) throws IOException {
		if (!file.setLastModified(time)) {
			throw new FileUtilException("Unable to set last modified on file: " + file.getAbsolutePath());
		}
	}

	// public static void setTesting(final boolean testing) {
	// FileUtil.testing = testing;
	// }
	//
	// public static boolean isTesting() {
	// return testing;
	// }

	public static String getCanonicalPath(final File file) {
		try {
			return file.getCanonicalPath();
		} catch (final IOException e) {
		}

		return file.getAbsolutePath();
	}
}
