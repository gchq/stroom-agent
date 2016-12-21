package stroom.agent.dispatch;

import stroom.agent.util.zip.StroomZipRepository;

public interface Dispatcher {

	public boolean process(StroomZipRepository stroomZipRepository);
	
	public boolean isReadOnly();
	
}
