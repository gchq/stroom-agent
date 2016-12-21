package stroom.agent.util.logging;

import java.util.IllegalFormatException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Wrapper around log4j to do guarded logging with expression evaluation. E.g.
 * StroomLogger LOGGER.logInfo("copy() %s %s", file1, file2);
 * 
 * also if you have noisy logging (e.g. "Written 2/100") you can use
 * xxxInterval(...) method to only log out every second.
 * 
 */
public final class StroomLogger {
	private static final String FQCN = StroomLogger.class.getName();
	private final Logger logger;

	private long interval = 0;
	private long nextLogTime = 0;

	// Use a private constructor as this is only made via the static factory.
	private StroomLogger(final Logger logger) {
		this.logger = logger;
	}

	public final static StroomLogger getLogger(final Class<?> clazz) {
		final Logger logger = Logger.getLogger(clazz.getName());
		final StroomLogger stroomLogger = new StroomLogger(logger);
		stroomLogger.interval = 1000;
		return stroomLogger;
	}

	public boolean checkInterval() {
		if (interval == 0) {
			return true;
		}
		// We don't care about thread race conditions ...
		long time = System.currentTimeMillis();
		if (time > nextLogTime) {
			nextLogTime = time + interval;
			return true;
		}
		return false;

	}

	public void setInterval(final long interval) {
		this.interval = interval;
	}

	public void debugInterval(final Object... args) {
		if (isDebugEnabled()) {
			if (checkInterval()) {
				logger.log(FQCN, Level.DEBUG, buildMessage(args),
						extractThrowable(args));
			}
		}
	}

	public void traceInterval(final Object... args) {
		if (isTraceEnabled()) {
			if (checkInterval()) {
				logger.log(FQCN, Level.TRACE, buildMessage(args),
						extractThrowable(args));
			}
		}
	}

	public void infoInterval(final Object... args) {
		if (isInfoEnabled()) {
			if (checkInterval()) {
				logger.log(FQCN, Level.INFO, buildMessage(args),
						extractThrowable(args));
			}
		}
	}

	public String buildMessage(final Object... args) {
		IllegalFormatException ilEx = null;
		try {
			if (args[0] instanceof String) {
				if (args.length > 1) {
					final Object[] otherArgs = new Object[args.length - 1];
					System.arraycopy(args, 1, otherArgs, 0, otherArgs.length);
					return String.format((String) args[0], otherArgs);
				} else {
					return (String) args[0];
				}
			}
		} catch (IllegalFormatException il) {
			ilEx = il;
		}
		final StringBuilder builder = new StringBuilder();
		if (ilEx != null) {
			builder.append(ilEx.getMessage());
		}
		for (final Object arg : args) {
			if (builder.length() > 0) {
				builder.append(" - ");
			}
			builder.append(String.valueOf(arg));
		}
		return builder.toString();
	}

	public Throwable extractThrowable(final Object... args) {
		if (args.length > 0) {
			if (args[args.length - 1] instanceof Throwable) {
				return (Throwable) args[args.length - 1];
			}
		}
		return null;
	}

	public void trace(final Object... args) {
		if (isTraceEnabled()) {
			logger.log(FQCN, Level.TRACE, buildMessage(args),
					extractThrowable(args));
		}
	}

	public void debug(final Object... args) {
		if (isDebugEnabled()) {
			logger.log(FQCN, Level.DEBUG, buildMessage(args),
					extractThrowable(args));
		}
	}

	public void info(final Object... args) {
		if (isInfoEnabled()) {
			logger.log(FQCN, Level.INFO, buildMessage(args),
					extractThrowable(args));
		}
	}

	public void warn(final Object... args) {
		logger
				.log(FQCN, Level.WARN, buildMessage(args),
						extractThrowable(args));
	}

	public void error(final Object... args) {
		logger.log(FQCN, Level.ERROR, buildMessage(args),
				extractThrowable(args));
	}

	public void fatal(final Object... args) {
		logger.log(FQCN, Level.FATAL, buildMessage(args),
				extractThrowable(args));
	}

	public boolean isTraceEnabled() {
		return logger!=null && logger.isTraceEnabled();
	}

	public boolean isDebugEnabled() {
		return logger!=null && logger.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return logger!=null && logger.isInfoEnabled();
	}

	public void log(final String callerFQCN, final Priority level,
			final Object message, final Throwable t) {
		logger.log(callerFQCN, level, message, t);
	}
}
