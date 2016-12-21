package stroom.agent.util.shared;

/**
 * <p>
 * Used by class's that have a string key and value (e.g. used to populate drop
 * downs etc).
 * </p>
 * 
 * @author Not attributable
 */
public interface HasDisplayValue {
	/**
	 * the string label/description of this object.
	 * 
	 * @return getter
	 */
	String getDisplayValue();
}
