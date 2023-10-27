package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public class QueryCondition implements QueryItem {
	private final String key;
	private final String comparator;
	private final Object value;
	
	public QueryCondition(String key, String comparator, Object value) {
		this.key = key;
		this.comparator = comparator;
		this.value = value;
	}
	
	public void generateQuerry(StringBuilder querry, String tableName) {
		querry.append(tableName);
		querry.append(".");
		querry.append(this.key);
		querry.append(" ");
		querry.append(this.comparator);
		querry.append(" ?");
		
	}
	
	@Override
	public int injectQuerry(PreparedStatement ps, int iii) throws Exception {
		DataAccess.addElement(ps, this.value, iii++);
		return iii;
	}
}
