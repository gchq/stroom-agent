package stroom.agent.util.shared;

import java.io.Serializable;

public interface TaskId extends Serializable {
	TaskId getParentId();

	boolean hasAncestor(TaskId id);

	boolean isOrHasAncestor(TaskId id);
}
