package stroom.agent.collect;

import stroom.agent.util.zip.StroomZipRepository;

import java.util.Date;

public interface Collector {

	String getFeed(); 

	void process(StroomZipRepository stroomZipRepository, Date dateFrom);
}
