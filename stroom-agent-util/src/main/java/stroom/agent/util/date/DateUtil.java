package stroom.agent.util.date;

import stroom.agent.util.logging.StroomLogger;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * <p>
 * Utility Class for some date stuff.
 * </p>
 *
 * @author Not attributable
 */
public final class DateUtil {
	static {
		// Set the default timezone and locale for all date time operations.
		DateTimeZone.setDefault(DateTimeZone.UTC);
		Locale.setDefault(Locale.ROOT);
	}

	private static final StroomLogger LOGGER = StroomLogger.getLogger(DateUtil.class);
	private static final String NULL = "NULL";
	public static final DateTimeFormatter NORMAL_STROOM_TIME_FORMATTER = DateTimeFormat.forPattern(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(DateTimeZone.UTC);
	private static final DateTimeFormatter FILE_TIME_STROOM_TIME_FORMATTER = DateTimeFormat.forPattern(
			"yyyy-MM-dd'T'HH#mm#ss,SSS'Z'").withZone(DateTimeZone.UTC);
	private static final String GMT_BST_GUESS = "GMT/BST";
	private static final DateTimeZone EUROPE_LONDON_TIME_ZONE = DateTimeZone.forID("Europe/London");

	public final static int DATE_LENGTH = "2000-01-01T00:00:00.000Z".length();

	public final static long MIN_MS = 1000 * 60;
	public final static long HOUR_MS = MIN_MS * 60;
	public final static long DAY_MS = HOUR_MS * 24;

	private DateUtil() {
		// Private constructor.
	}

	/**
	 * Parse a date using a format.
	 *
	 * @param lastFormat
	 *            optional last format to use
	 * @param pattern
	 *            pattern to match
	 * @param timeZoneId
	 *            if provided the pattern will append Z and the time zone will be added
	 * @param value
	 *            value to parse
	 * @return the result
	 * @throws IllegalArgumentException
	 *             if date does not parse
	 */
	public static long parseDate(final String pattern, final String timeZoneId, final String value) {
		DateTimeZone dateTimeZone = null;
		DateTime dateTime = null;

		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException("Unable to parse date: \"" + value + '"');
		}

		// final Set<String> zones = DateTimeZone.getAvailableIDs();
		// for (final String zone : zones) {
		// System.out.println(zone);
		// }

		// Try to parse the time zone first.
		try {
			if (timeZoneId != null) {
				if (GMT_BST_GUESS.equals(timeZoneId)) {
					dateTimeZone = EUROPE_LONDON_TIME_ZONE;
				} else {
					dateTimeZone = DateTimeZone.forID(timeZoneId);
				}
			}
		} catch (final IllegalArgumentException e) {
			LOGGER.debug(e, e);
		}

		DateTimeFormatter dateFormat = buildDateFormatter(pattern);
		if (dateTimeZone != null) {
			try {
				dateFormat = dateFormat.withZone(dateTimeZone);
				dateTime = dateFormat.parseDateTime(value);

			} catch (final IllegalArgumentException e) {
				LOGGER.debug(e, e);

				// We failed to use the time zone so try UTC.
				dateFormat = dateFormat.withZone(DateTimeZone.UTC);
				dateTime = dateFormat.parseDateTime(value);
			}
		} else {
			dateTime = dateFormat.parseDateTime(value);
		}

		if (dateTime == null) {
			throw new IllegalArgumentException("Unable to parse date: \"" + value + '"');
		}

		return dateTime.getMillis();
	}

	/**
	 * Build a simple date format.
	 *
	 * @param format
	 * @return
	 */
	private static DateTimeFormatter buildDateFormatter(final String pattern) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(pattern);
		return dateTimeFormatter.withZone(DateTimeZone.UTC);
	}

	/**
	 * Create a 'normal' type date with the current system time.
	 *
	 * @param ms
	 *            the date
	 * @return string value
	 */
	public static String createNormalDateTimeString() {
		return NORMAL_STROOM_TIME_FORMATTER.print(System.currentTimeMillis());
	}

	/**
	 * Create a 'normal' type date.
	 *
	 * @param ms
	 *            the date
	 * @return string value
	 */
	public static String createNormalDateTimeString(final Long ms) {
		if (ms == null) {
			return "";
		}
		return NORMAL_STROOM_TIME_FORMATTER.print(ms);
	}

	/**
	 * Create a 'file' format date string witht he current system time.
	 *
	 * @param ms
	 *            The date to create the string for.
	 * @return string The date as a 'file' format date string.
	 */
	public static String createFileDateTimeString() {
		return FILE_TIME_STROOM_TIME_FORMATTER.print(System.currentTimeMillis());
	}

	/**
	 * Create a 'file' format date string.
	 *
	 * @param ms
	 *            The date to create the string for.
	 * @return string The date as a 'file' format date string.
	 */
	public static String createFileDateTimeString(final long ms) {
		return FILE_TIME_STROOM_TIME_FORMATTER.print(ms);
	}

	/**
	 * Parse a 'normal' type date.
	 *
	 * @param date
	 *            string date
	 * @return date as milliseconds since epoch
	 * @throws IllegalArgumentException
	 *             if date does not parse
	 */
	public static long parseNormalDateTimeString(final String date) {
		if (date == null || date.length() != DATE_LENGTH) {
			throw new IllegalArgumentException("Unable to parse date: \"" + date + '"');
		}

		final DateTime dateTime = NORMAL_STROOM_TIME_FORMATTER.parseDateTime(date);
		if (dateTime == null) {
			throw new IllegalArgumentException("Unable to parse date: \"" + date + '"');
		}

		return dateTime.getMillis();
	}

	public static long parseUnknownString(final String date) {
		if (date == null || date.length() != DATE_LENGTH) {
			Long.parseLong(date);
		}
		
		try {
			/*
			 * Try and parse the string as a standard ISO8601 date.
			 */
			final DateTime dateTime = NORMAL_STROOM_TIME_FORMATTER.parseDateTime(date);
			return dateTime.getMillis();

		} catch (final Exception e) {
			/*
			 * If we were unable to parse the value as an ISO8601 date then try and get it as a long.
			 */
			return Long.parseLong(date);
		}
	}
}
