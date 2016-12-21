package stroom.agent.main;

import stroom.agent.collect.Collector;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class StroomAgentConfig {
	private Pattern feed;
	private Pattern notFeed;
	
	public boolean okToRun(Collector collector) {
		return okToRun(collector.getFeed());
	}
	public boolean okToRun(String aFeed) {
		if (feed != null) {
			return feed.matcher(aFeed).matches();
		}
		if (notFeed != null) {
			return !notFeed.matcher(aFeed).matches();
		}
		return true;
	}
	
	public void setFeed(String feedPattern) {
		if (StringUtils.hasText(feedPattern)) {
			this.feed = Pattern.compile(feedPattern);	
		}
	}
	public void setNotFeed(String feedPattern) {
		if (StringUtils.hasText(feedPattern)) {
			this.notFeed = Pattern.compile(feedPattern);	
		}
	}

}
