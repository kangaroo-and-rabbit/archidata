package org.atriasoft.archidata.dataAccess.options;

import java.util.List;

import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.bson.Document;

public class OrderBy extends QueryOption {
	protected final List<OrderItem> childs;

	public OrderBy(final List<OrderItem> childs) {
		this.childs = childs;
	}

	public OrderBy(final OrderItem... childs) {
		this.childs = List.of(childs);
	}

	public void generateSort(final Document data) {
		for (final OrderItem elem : this.childs) {
			data.append(elem.value, elem.order == Order.ASC ? 1 : -1);
		}
	}
}
