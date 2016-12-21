package stroom.agent.util.zip;


/**
 * Utility to build a path for a given id in such a way that we don't every
 * exceed OS dir file limits.
 * 
 * 
 * So file 1 is 001 and file 1000 is 001/000 etc
 */
public class StroomFileNameUtil {
	/**
	 * @param id
	 * @return
	 */
	public static String getDirPathForId(long id) {
		return buildPath(id, true);
	}

	/**
	 * Build a file path for a id.
	 * 
	 * @param id
	 * @return
	 */
	public static String getFilePathForId(long id) {
		return buildPath(id, false);
	}

	private static String buildPath(long id, boolean justDir) {
		StringBuilder fileBuffer = new StringBuilder();
		fileBuffer.append(id);
		// Pad out e.g. 10100 -> 010100
		while ((fileBuffer.length() % 3) != 0) {
			fileBuffer.insert(0, '0');
		}
		StringBuilder dirBuffer = new StringBuilder();
		for (int i = 0; i < fileBuffer.length() - 3; i += 3) {
			dirBuffer.append(fileBuffer.subSequence(i, i + 3));
			dirBuffer.append("/");
		}

		if (justDir) {
			return dirBuffer.toString();
		} else {
			return dirBuffer.toString() + fileBuffer.toString();
		}
	}

}
