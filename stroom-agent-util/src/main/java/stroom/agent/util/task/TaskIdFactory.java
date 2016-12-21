package stroom.agent.util.task;

import stroom.agent.util.shared.TaskId;
import stroom.agent.util.shared.TaskIdImpl;

import java.util.UUID;

public class TaskIdFactory {
	public static TaskId create() {
		return new TaskIdImpl(createUUID(), null);
	}

	public static TaskId create(final TaskId parentTaskId) {
		if (parentTaskId != null) {
			return new TaskIdImpl(createUUID(), parentTaskId);
		}

		return create();
	}

	private static String createUUID() {
		return UUID.randomUUID().toString();
	}
}
