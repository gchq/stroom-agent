package stroom.agent.util.task;

import stroom.agent.util.shared.TerminateHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExternalShutdownController {
	private static final Map<Object, TerminateHandler> terminateHandlers = new ConcurrentHashMap<Object, TerminateHandler>();

	public static void addTerminateHandler(final Object key,
			final TerminateHandler terminateHandler) {
		terminateHandlers.put(key, terminateHandler);
	}

	public static void shutdown() {
		for (final TerminateHandler terminateHandler : terminateHandlers
				.values()) {
			terminateHandler.onTerminate();
		}
	}
}
