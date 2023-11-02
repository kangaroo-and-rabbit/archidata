package org.kar.archidata.dataAccess;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryOptions {
	static final Logger LOGGER = LoggerFactory.getLogger(QueryOptions.class);
	public static final String SQL_NOT_READ_DISABLE = "SQLNotRead_disable";
	public static final String SQL_DELETED_DISABLE = "SQLDeleted_disable";
	public static final String OVERRIDE_TABLE_NAME = "SQL_OVERRIDE_TABLE_NAME";
	public static final String CREATE_DROP_TABLE = "CREATE_DROP_TABLE";
	
	private final Map<String, Object> options = new HashMap<>();
	
	public QueryOptions() {
		
	}
	
	public QueryOptions(final String key, final Object value) {
		this.options.put(key, value);
	}
	
	public QueryOptions(final String key, final Object value, final String key2, final Object value2) {
		this.options.put(key, value);
		this.options.put(key2, value2);
	}
	
	public QueryOptions(final String key, final Object value, final String key2, final Object value2, final String key3, final Object value3) {
		this.options.put(key, value);
		this.options.put(key2, value2);
		this.options.put(key3, value3);
	}
	
	public void put(final String key, final Object value) {
		this.options.put(key, value);
	}
	
	public Object get(final String value) {
		return this.options.get(value);
	}

	public static boolean readAllFields(final QueryOptions options) {
		if (options != null) {
			final Object data = options.get(QueryOptions.SQL_NOT_READ_DISABLE);
			if (data instanceof final Boolean elem) {
				return elem;
			} else if (data != null) {
				LOGGER.error("'{}' ==> has not a boolean value: {}", QueryOptions.SQL_NOT_READ_DISABLE, data);
			}
		}
		return false;
	}
	
}
