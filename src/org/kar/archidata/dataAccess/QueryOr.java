package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

public class QueryOr implements QueryItem {
	protected final List<QueryItem> childs;
	
	public QueryOr(List<QueryItem> childs) {
		this.childs = childs;
	}
	
	public void generateQuerry(StringBuilder querry, String tableName) {
		if (this.childs.size() >= 1) {
			querry.append(" (");
		}
		boolean first = true;
		for (QueryItem elem : this.childs) {
			if (first) {
				first = false;
			} else {
				querry.append(" OR ");
			}
			elem.generateQuerry(querry, tableName);
		}
		if (this.childs.size() >= 1) {
			querry.append(")");
		}
	}
	
	@Override
	public int injectQuerry(PreparedStatement ps, int iii) throws Exception {
		
		for (QueryItem elem : this.childs) {
			iii = elem.injectQuerry(ps, iii);
		}
		return iii;
	}
}
