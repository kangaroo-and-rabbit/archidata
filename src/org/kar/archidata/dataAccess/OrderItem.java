package org.kar.archidata.dataAccess;

public class OrderItem {
	public enum Order {
		ASC, DESC
	};

	public final String value;
	public final Order order;

	public OrderItem(final String value, final Order order) {
		this.value = value;
		this.order = order;
	}

}
