package org.kar.archidata.dataAccess;

import java.util.HashMap;
import java.util.Map;

public class QueryOptions {
	public static final String SQL_NOT_READ_DISABLE = "SQLNotRead_disable";
	public static final String SQL_DELETED_DISABLE = "SQLDeleted_disable";
	
	private final Map<String, Object> options = new HashMap<>();
	
	public QueryOptions() {
		
	}
	
	public QueryOptions(String key, Object value) {
		this.options.put(key, value);
	}
	
	public QueryOptions(String key, Object value, String key2, Object value2) {
		this.options.put(key, value);
		this.options.put(key2, value2);
	}
	
	public QueryOptions(String key, Object value, String key2, Object value2, String key3, Object value3) {
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
