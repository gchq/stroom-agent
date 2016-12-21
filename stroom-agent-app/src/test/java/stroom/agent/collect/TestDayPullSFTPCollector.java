package stroom.agent.collect;

import stroom.agent.util.test.StroomJUnit4ClassRunner;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(StroomJUnit4ClassRunner.class)
public class TestDayPullSFTPCollector {
	@Test
	public void testDaysOld() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, -1);
		
		Assert.assertEquals(1, new DayPullSFTPCollector().daysOld(calendar.getTime()));
		Assert.assertEquals(0, new DayPullSFTPCollector().daysOld(new Date()));
	}
	
	@Test
	public void testExpectedMatches() {
		DayPullSFTPCollector dayPullSFTPCollector = new DayPullSFTPCollector();
		Assert.assertEquals(1, dayPullSFTPCollector.getMinExpectedMatches().intValue());
		Assert.assertEquals(1, dayPullSFTPCollector.getMaxExpectedMatches().intValue());
		
		dayPullSFTPCollector.setExpectedMatches(3);
		Assert.assertEquals(3, dayPullSFTPCollector.getMinExpectedMatches().intValue());
		Assert.assertEquals(3, dayPullSFTPCollector.getMaxExpectedMatches().intValue());
		
		dayPullSFTPCollector.setMinExpectedMatches(2);
		Assert.assertEquals(2, dayPullSFTPCollector.getMinExpectedMatches().intValue());
		Assert.assertEquals(3, dayPullSFTPCollector.getMaxExpectedMatches().intValue());
		
		dayPullSFTPCollector.setMinExpectedMatches(5);
		Assert.assertEquals(5, dayPullSFTPCollector.getMinExpectedMatches().intValue());
		Assert.assertNull(dayPullSFTPCollector.getMaxExpectedMatches());

		dayPullSFTPCollector.setMaxExpectedMatches(3);
		Assert.assertNull(dayPullSFTPCollector.getMinExpectedMatches());
		Assert.assertEquals(3, dayPullSFTPCollector.getMaxExpectedMatches().intValue());
		

		
	}
}
