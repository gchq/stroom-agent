package stroom.agent.collect;

import java.io.File;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Ignore;

import stroom.agent.util.test.StroomJUnit4ClassRunner;
import stroom.agent.util.test.FileSystemTestUtil;
import stroom.agent.util.io.FileUtil;
import stroom.agent.util.zip.StroomZipRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.jcraft.jsch.SftpException;

@Ignore
@RunWith(StroomJUnit4ClassRunner.class)
public class TestHeadlessStroomFileProcessor {
	@Test
	public void testFileCheck() throws Exception {

		File testFile = 
				new File(
				FileUtil.getTempDir(),
				FileSystemTestUtil.getUniqueTestString());

		StroomZipRepository stroomZipRepository = new StroomZipRepository(testFile.getAbsolutePath()); 
		
		//TODO create test data
		final String fileName = "headless/...";  
		
		HeadlessStroomFileProcessor headlessStroomFileProcessor = new HeadlessStroomFileProcessor() {
			@Override
			public InputStream getRemoteStream(SFTPTransfer sftpTransfer,
					SFTPFileDetails fileDetails) throws SftpException {
				return STROOMAgentTestFileUtil.getInputStream(fileName); 
			}
			public void deleteRemoteFile(SFTPTransfer sftpTransfer, SFTPFileDetails fileDetails) throws SftpException {
				
			};
		};
		headlessStroomFileProcessor.processFile(
				null,
				stroomZipRepository,
				new SFTPFileDetails("", fileName, 0),
				false,
				null);
		
		Assert.assertEquals(1, stroomZipRepository.getFileCount());
	}

	@Test
	public void testError() throws Exception {

		File testFile = 
				new File(
				FileUtil.getTempDir(),
				FileSystemTestUtil.getUniqueTestString());

		StroomZipRepository stroomZipRepository = new StroomZipRepository(testFile.getAbsolutePath()); 
		
		//TODO create test data
		final String fileName = "headless/......";  
		
		HeadlessStroomFileProcessor headlessStroomFileProcessor = new HeadlessStroomFileProcessor() {
			@Override
			public InputStream getRemoteStream(SFTPTransfer sftpTransfer,
					SFTPFileDetails fileDetails) throws SftpException {
				return STROOMAgentTestFileUtil.getInputStream(fileName); 
			}
			public void deleteRemoteFile(SFTPTransfer sftpTransfer, SFTPFileDetails fileDetails) throws SftpException {
				
			};
		};
		headlessStroomFileProcessor.setStroomZipRepository(stroomZipRepository);
		headlessStroomFileProcessor.processStream(
				STROOMAgentTestFileUtil.getInputStream(fileName)
		);
		
		
		Assert.assertEquals(0, stroomZipRepository.getFileCount());
		Assert.assertEquals(0, headlessStroomFileProcessor.getFileCount());
		Assert.assertEquals(0, headlessStroomFileProcessor.getFilePartCount());
		Assert.assertEquals(1, headlessStroomFileProcessor.getErrorCount());
		
	}
	
	@Test
	public void testReplace1() throws Exception {
		HeadlessStroomFileProcessor processor = new HeadlessStroomFileProcessor();
		processor.setFeedMatch("WHOLE_FEED".split(","));
		processor.setFeedReplace("REPLACE".split(","));
		Assert.assertEquals("REPLACE", processor.feedReplace("WHOLE_FEED"));
	}
	@Test
	public void testReplace2() throws Exception {
		HeadlessStroomFileProcessor processor = new HeadlessStroomFileProcessor();
		processor.setFeedMatch("A1,A2".split(","));
		processor.setFeedReplace("REPLACE".split(","));
		Assert.assertEquals("REPLACE", processor.feedReplace("A1"));
	}
	@Test
	public void testReplace3() throws Exception {
		HeadlessStroomFileProcessor processor = new HeadlessStroomFileProcessor();
		processor.setFeedMatch("A1,A2".split(","));
		processor.setFeedReplace("REPLACE".split(","));
		Assert.assertEquals("A2", processor.feedReplace("A2"));
	}
	@Test
	public void testReplace4() throws Exception {
		HeadlessStroomFileProcessor processor = new HeadlessStroomFileProcessor();
		processor.setFeedMatch("A1,A2".split(","));
		processor.setFeedReplace("REPLACE".split(","));
		try {
			processor.feedReplace("A4");
			Assert.fail("Expecting exception");
		} catch (Exception exception) {
			
		}
	}
}
