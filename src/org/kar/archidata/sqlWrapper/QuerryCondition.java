package org.kar.archidata.sqlWrapper;

import java.sql.PreparedStatement;

public class QuerryCondition implements QuerryItem {
	private final String key;
	private final String comparator;
	private final Object value;
	
	public QuerryCondition(String key, String comparator, Object value) {
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
		SqlWrapper.addElement(ps, this.value, iii++);
		return iii;
	}
}
