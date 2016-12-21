package stroom.agent.collect;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;

import stroom.agent.util.io.StreamUtil;
import stroom.agent.util.test.StroomJUnit4ClassRunner;
import stroom.agent.util.io.FileUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(StroomJUnit4ClassRunner.class)
public class TestFilePersistUtil {
	@Test
	public void test() throws IOException {
		String testFile = FileUtil.getTempDir() + "/TestFilePersistUtil";
		File lockFile = new File(testFile + ".lock");
		lockFile.delete();
		
		FilePersistUtil.commitFile(testFile, "1\n2\n3\n");
		FilePersistUtil.commitFile(testFile, "1\n2\n3\n4\n");
		
		Assert.assertEquals("1\n2\n3\n4\n", StreamUtil.fileToString(new File(testFile)));
		
		lockFile.createNewFile();
		lockFile.setWritable(false);

		try {
			FilePersistUtil.commitFile(testFile, "1\n");
		} catch (IOException ex) {
		}
		Assert.assertEquals("1\n2\n3\n4\n", StreamUtil.fileToString(new File(testFile)));
	}
}
