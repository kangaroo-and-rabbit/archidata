package org.kar.archidata.sqlWrapper;

import java.util.HashMap;
import java.util.Map;

public class QuerryOptions {
	private final Map<String, Object> options = new HashMap<>();
	
	public QuerryOptions() {
		
	}
	
	public QuerryOptions(String key, Object value) {
		options.put(key, value);
	}
	
	public QuerryOptions(String key, Object value, String key2, Object value2) {
		options.put(key, value);
		options.put(key2, value2);
	}
	
	public QuerryOptions(String key, Object value, String key2, Object value2, String key3, Object value3) {
		options.put(key, value);
		options.put(key2, value2);
		options.put(key3, value3);
	}
	
	public void put(String key, Object value) {
		options.put(key, value);
	}
	
	public Object get(String value) {
		return options.get(value);
	}
}
