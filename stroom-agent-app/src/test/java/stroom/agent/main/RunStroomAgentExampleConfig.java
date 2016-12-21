package stroom.agent.main;

import java.io.File;

public class RunStroomAgentExampleConfig {
	public static void main(String[] args) throws Exception {
		
		String pwd = new File(".").getCanonicalPath();
		System.setProperty("PWD", pwd);
		
		
		StroomAgent stroomAgent = new StroomAgent();
		stroomAgent.setConfigFile("src/test/resources/test-data/ExampleConfig.xml");
		stroomAgent.doMain(new String[] {});

	}
}
