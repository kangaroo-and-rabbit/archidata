package org.kar.archidata.sqlWrapper;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class QuerryAnd implements QuerryItem {
	protected final List<QuerryItem> childs;
	
	public QuerryAnd(List<QuerryItem> childs) {
		this.childs = childs;
	}
	
	public QuerryAnd(QuerryItem... items) {
		this.childs = new ArrayList<>();
		for (int iii = 0; iii < items.length; iii++) {
			this.childs.add(items[iii]);
		}
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
				querry.append(" AND ");
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
