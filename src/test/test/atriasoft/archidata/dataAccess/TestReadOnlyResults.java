package test.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.commonTools.RepairReport;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The containers of the data layer keep their content: a list goes in or out as a copy or a read-only view. */
class TestReadOnlyResults {

	@Test
	void queryOptionsGetAllIsAReadOnlyView() {
		final QueryOptions options = new QueryOptions();
		final List<?> all = options.getAll();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> options.getAll().add(new Limit(1)));
		options.add(new Limit(1));
		Assertions.assertEquals(1, all.size());
	}

	@Test
	void repairReportDetailsAreAReadOnlyView() {
		final RepairReport report = new RepairReport();
		report.addDetail("one");
		Assertions.assertEquals(List.of("one"), report.getDetails());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> report.getDetails().add("two"));
	}

	@Test
	void orderByCopiesItsItems() {
		final List<OrderItem> items = new ArrayList<>(List.of(new OrderItem("name", Order.ASC)));
		final OrderBy order = new OrderBy(items);
		items.add(new OrderItem("age", Order.DESC));
		final Document sort = new Document();
		order.generateSort(sort);
		Assertions.assertEquals(1, sort.size());
		Assertions.assertTrue(sort.containsKey("name"));
	}
}
