package stroom.agent.collect;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class FilePersistUtil {
	public final static String LOCK_EXTENSION = ".lock";
	
	/**
	 * Utility to write a file to disk and if it fails leave the original file there 
     *
	 * @param fileName
	 * @param contents
	 * @throws IOException
	 */
	public static void commitFile(String fileName, String contents) throws IOException {
		File file = new File(fileName);
		File lockFile = new File(fileName + LOCK_EXTENSION);
		PrintWriter printWriter = new PrintWriter(lockFile);
		printWriter.write(contents);
		printWriter.close();
		file.delete();
		lockFile.renameTo(file);
	}

}
