package stroom.agent.util.shared;

/**
 * Interface to all tasks.
 *
 * @author Not attributable
 */
public interface Task<R> extends HasTerminate {
	TaskId getId();

	String getTaskName();

	String getSessionId();

	String getUserId();

	ThreadPool getThreadPool();
}
