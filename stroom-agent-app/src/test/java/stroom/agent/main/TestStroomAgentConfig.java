package stroom.agent.main;

import org.junit.Assert;
import org.junit.Test;

public class TestStroomAgentConfig {
	@Test
	public void testSimple() {
		doTest(".*", "A", true);
		doTest("", "A", true);
		doTest("A", "A", true);
		doTest("A", "B", false);
	}

	@Test
	public void testMatch() {
		doTest("(ABC)|(DEF)", "ABC", true);
		doTest("(ABC)|(DEF)", "DEF", true);
	}
	@Test
	public void testNotMatch() {
		doNotTest("(.*-XYZ.*)|(.*DEF.*)", "ABC", true);
		doNotTest("(.*-XYZ.*)|(.*DEF.*)", "HJKL-XYZ-FEED", false);
		doNotTest("(.*-XYZ.*)|(.*DEF.*)", "HJKL-DEF-FEED", false);
		doNotTest("(.*-XYZ.*)|(.*DEF.*)", "XYZ-FEED", true);
	}
	
	private void doTest(String pattern, String feed, boolean ok) {
		StroomAgentConfig stroomAgentConfig = new StroomAgentConfig();
		stroomAgentConfig.setFeed(pattern);
		Assert.assertEquals(ok, stroomAgentConfig.okToRun(feed));
	}
	private void doNotTest(String pattern, String feed, boolean ok) {
		StroomAgentConfig stroomAgentConfig = new StroomAgentConfig();
		stroomAgentConfig.setNotFeed(pattern);
		Assert.assertEquals(ok, stroomAgentConfig.okToRun(feed));
	}
	
}
