package stroom.agent.main;

import stroom.agent.collect.HeaderMapPopulator;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.zip.HeaderMap;

/**
 * stroom.agent.main.StroomAgent configFile=<PATH>
 */
public class StroomAgentTestHeaderMapPopulator extends StroomAgentBase {
	
	private final static StroomLogger LOGGER = StroomLogger.getLogger(StroomAgentTestHeaderMapPopulator.class); 

	private String testBean = null;
	private HeaderMap headerMap = null;
	
	public void setTestBean(String testBean) {
		this.testBean = testBean;
	}
	
	public static void main(String[] args) throws Exception {
		new StroomAgentTestHeaderMapPopulator().doMain(args);
	}
	
	@Override
	public void doMain(String[] args) throws Exception {
		headerMap = new HeaderMap();
		headerMap.loadArgs(args);
		super.doMain(args);
	}
	

	@Override
	public void doRun() {
			
		HeaderMapPopulator headerMapPopulator = (HeaderMapPopulator)getApplicationContext().getBean(testBean);
		
		LOGGER.info("doRun() - Header Map Before %s", headerMap);
		
		headerMapPopulator.populateHeaderMap(headerMap);
					
		LOGGER.info("doRun() - Header Map After %s", headerMap);
		
		
	}

}
