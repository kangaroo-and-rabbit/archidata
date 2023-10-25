package org.kar.archidata.sqlWrapper;

import java.util.HashMap;
import java.util.Map;

public class QuerryOptions {
	public static final String SQL_NOT_READ_DISABLE = "SQLNotRead_disable";
	public static final String SQL_DELETED_DISABLE = "SQLDeleted_disable";
	
	private final Map<String, Object> options = new HashMap<>();
	
	public QuerryOptions() {
		
	}
	
	public QuerryOptions(String key, Object value) {
		this.options.put(key, value);
	}
	
	public QuerryOptions(String key, Object value, String key2, Object value2) {
		this.options.put(key, value);
		this.options.put(key2, value2);
	}
	
	public QuerryOptions(String key, Object value, String key2, Object value2, String key3, Object value3) {
		this.options.put(key, value);
		this.options.put(key2, value2);
		this.options.put(key3, value3);
	}
	
	public void put(String key, Object value) {
		this.options.put(key, value);
	}
	
	public Object get(String value) {
		return this.options.get(value);
	}
	
}
