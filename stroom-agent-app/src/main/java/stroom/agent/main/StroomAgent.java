package stroom.agent.main;

import stroom.agent.collect.Collector;
import stroom.agent.dispatch.Dispatcher;
import stroom.agent.util.date.DateUtil;
import stroom.agent.util.logging.StroomLogger;
import stroom.agent.util.logging.LogExecutionTime;
import stroom.agent.util.zip.StroomZipRepository;

import java.util.Date;

/**
 * stroom.agent.main.StroomAgent configFile=<PATH>
 */
public class StroomAgent extends StroomAgentBase {
	
	private final static StroomLogger LOGGER = StroomLogger.getLogger(StroomAgent.class); 
	
	private Date fromDate = null;
	
	public void setFromDate(String fromDate) {
		this.fromDate = new Date(DateUtil.parseNormalDateTimeString(fromDate));
	}
	
	public static void main(String[] args) throws Exception {
		new StroomAgent().doMain(args);
	}
	

	@Override
	public void doRun() {
			
		StroomZipRepository stroomZipRepository = getApplicationContext().getBean(StroomZipRepository.class);
		StroomAgentConfig stroomAgentConfig = null;
		if (getApplicationContext().getBeanNamesForType(StroomAgentConfig.class).length == 1) {
			stroomAgentConfig = getApplicationContext().getBean(StroomAgentConfig.class);
		}		
		String[] collectBeanList = getApplicationContext().getBeanNamesForType(Collector.class);
		
		for (String collectBean : collectBeanList) {
			Collector collector = (Collector)getApplicationContext().getBean(collectBean);
			if (stroomAgentConfig == null || stroomAgentConfig.okToRun(collector)) {
				LogExecutionTime logExecutionTime = new LogExecutionTime();
				LOGGER.info("run() - %s Start ", collector);
				try {
					collector.process(stroomZipRepository, fromDate);
				} catch (Exception ex) {
					LOGGER.error("run()", ex);
				}
				LOGGER.info("run() - %s Finished in %s", collector, logExecutionTime);
			} else {
				LOGGER.info("run() - %s Skipping ", collector);
			}
		}

		String[] dispatchBeanList = getApplicationContext().getBeanNamesForType(Dispatcher.class);

		// Run the read only dispatch beans first
		for (String dispatchBean : dispatchBeanList) {
			Dispatcher dispatcher = (Dispatcher)getApplicationContext().getBean(dispatchBean);
			if (dispatcher.isReadOnly()) {
				LogExecutionTime logExecutionTime = new LogExecutionTime();
				LOGGER.info("run() - %s Start", dispatcher);
				try {
					dispatcher.process(stroomZipRepository);
				} catch (Exception ex) {
					LOGGER.error("run()", ex);
				}
				LOGGER.info("run() - %s Finished in %s", dispatcher, logExecutionTime);
			}
		}
		// Run the non-read only dispatch beans (that delete the files) ... only one is valid here
		for (String dispatchBean : dispatchBeanList) {
			Dispatcher dispatcher = (Dispatcher)getApplicationContext().getBean(dispatchBean);
			if (!dispatcher.isReadOnly()) {
				LogExecutionTime logExecutionTime = new LogExecutionTime();
				LOGGER.info("run() - %s Start", dispatcher);
				try {
					dispatcher.process(stroomZipRepository);
				} catch (Exception ex) {
					LOGGER.error("run()", ex);
				}
				LOGGER.info("run() - %s Finished in %s", dispatcher, logExecutionTime);
			}
		}
			
		
		
	}

}
