package stroom.agent.collect;

import stroom.agent.util.zip.HeaderMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderMapPopulatorEntry {
	private String key;
	private String value;
	private String sourceKey;
	private Pattern sourcePattern;
	
	public HeaderMapPopulatorEntry(String key, String value) {
		this.key = key;
		this.value = value;
	}
	public HeaderMapPopulatorEntry(String key, String value, String sourceKey, String sourcePattern) {
		this.key = key;
		this.value = value;
		this.sourceKey = sourceKey;
		this.sourcePattern = Pattern.compile(sourcePattern);
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue(HeaderMap headerMap) {
		StringBuilder valueBuffer = new StringBuilder();
		StringBuilder variableBuffer = new StringBuilder();
		boolean inVariable = false;
		Matcher sourceMatcher = null;
				
		if (sourceKey!=null && sourcePattern!=null) {
			String sourceValue = headerMap.get(sourceKey);
			if (sourceValue != null) {
				sourceMatcher = sourcePattern.matcher(sourceValue);
				sourceMatcher.find();
			}
		}
			
		
		for (int i=0; i<value.length(); i++) {
			char c = value.charAt(i); 
			if (c == '$') {
				i++;
				inVariable = true;
			} else 
			if (c == '}') {
				int pos = Integer.parseInt(variableBuffer.toString());
				if (sourceMatcher != null) {
					valueBuffer.append(sourceMatcher.group(pos));
				}
				inVariable = false;
				variableBuffer.setLength(0);
			} else {
				if (inVariable) {
					variableBuffer.append(c);
				} else {
					valueBuffer.append(c);
				}
			}
		}
		
		return valueBuffer.toString();
	}
	
	
	@Override
	public String toString() {
		return "key="+key+
				", value="+value+
				", sourceKey="+sourceKey+
				", sourcePattern="+sourcePattern;
	}
	
	
	
	
	
}
