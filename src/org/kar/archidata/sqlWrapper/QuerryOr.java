package org.kar.archidata.sqlWrapper;

import java.sql.PreparedStatement;
import java.util.List;

public class QuerryOr implements QuerryItem {
	protected final List<QuerryItem> childs;
	
	public QuerryOr(List<QuerryItem> childs) {
		this.childs = childs;
	}
	
	public void generateQuerry(StringBuilder querry, String tableName) {
		if (this.childs.size() >= 1) {
			querry.append(" (");
		}
		boolean first = true;
		for (QuerryItem elem : this.childs) {
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
		
		for (QuerryItem elem : this.childs) {
			iii = elem.injectQuerry(ps, iii);
		}
		return iii;
	}
}
