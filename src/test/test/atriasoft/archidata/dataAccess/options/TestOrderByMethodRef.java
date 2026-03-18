package test.atriasoft.archidata.dataAccess.options;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.SerializableFunction;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

/**
 * Unit tests for method reference support in {@link OrderItem} and {@link OrderBy}.
 * These tests do NOT require a running MongoDB instance.
 */
public class TestOrderByMethodRef {

	public static class Model {
		@Id
		public String _id;
		public String name;
		public int age;
		@Column(name = "full_name")
		public String fullName;

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(final int age) {
			this.age = age;
		}

		public String getFullName() {
			return this.fullName;
		}

		public void setFullName(final String fullName) {
			this.fullName = fullName;
		}
	}

	@BeforeEach
	public void clearCaches() {
		DbClassModel.clearCache();
		ClassModel.clearCache();
		MethodReferenceResolver.clearCache();
	}

	// ====================================================================
	// OrderItem with getter reference
	// ====================================================================

	@Test
	public void testOrderItem_getter() {
		final OrderItem item = new OrderItem(
				(SerializableFunction<Model, String>) Model::getName, Order.ASC);
		Assertions.assertEquals("name", item.value);
		Assertions.assertEquals(Order.ASC, item.order);
	}

	@Test
	public void testOrderItem_getter_desc() {
		final OrderItem item = new OrderItem(
				(SerializableFunction<Model, Integer>) Model::getAge, Order.DESC);
		Assertions.assertEquals("age", item.value);
		Assertions.assertEquals(Order.DESC, item.order);
	}

	@Test
	public void testOrderItem_columnRename() {
		final OrderItem item = new OrderItem(
				(SerializableFunction<Model, String>) Model::getFullName, Order.ASC);
		Assertions.assertEquals("full_name", item.value);
	}

	// ====================================================================
	// OrderItem with setter reference
	// ====================================================================

	@Test
	public void testOrderItem_setter() {
		final OrderItem item = new OrderItem(
				(SerializableBiConsumer<Model, String>) Model::setName, Order.DESC);
		Assertions.assertEquals("name", item.value);
		Assertions.assertEquals(Order.DESC, item.order);
	}

	@Test
	public void testOrderItem_setter_columnRename() {
		final OrderItem item = new OrderItem(
				(SerializableBiConsumer<Model, String>) Model::setFullName, Order.ASC);
		Assertions.assertEquals("full_name", item.value);
	}

	// ====================================================================
	// OrderBy factory methods
	// ====================================================================

	@Test
	public void testOrderBy_ascFactory_getter() {
		final OrderBy orderBy = OrderBy.asc(
				(SerializableFunction<Model, String>) Model::getName);
		final Document sort = new Document();
		orderBy.generateSort(sort);
		Assertions.assertEquals(1, sort.getInteger("name"));
	}

	@Test
	public void testOrderBy_descFactory_getter() {
		final OrderBy orderBy = OrderBy.desc(
				(SerializableFunction<Model, Integer>) Model::getAge);
		final Document sort = new Document();
		orderBy.generateSort(sort);
		Assertions.assertEquals(-1, sort.getInteger("age"));
	}

	@Test
	public void testOrderBy_ascFactory_setter() {
		final OrderBy orderBy = OrderBy.asc(
				(SerializableBiConsumer<Model, String>) Model::setName);
		final Document sort = new Document();
		orderBy.generateSort(sort);
		Assertions.assertEquals(1, sort.getInteger("name"));
	}

	@Test
	public void testOrderBy_descFactory_setter() {
		final OrderBy orderBy = OrderBy.desc(
				(SerializableBiConsumer<Model, Integer>) Model::setAge);
		final Document sort = new Document();
		orderBy.generateSort(sort);
		Assertions.assertEquals(-1, sort.getInteger("age"));
	}

	@Test
	public void testOrderBy_ascFactory_columnRename() {
		final OrderBy orderBy = OrderBy.asc(
				(SerializableFunction<Model, String>) Model::getFullName);
		final Document sort = new Document();
		orderBy.generateSort(sort);
		Assertions.assertEquals(1, sort.getInteger("full_name"));
	}

	// ====================================================================
	// Consistency: factory vs manual construction
	// ====================================================================

	@Test
	public void testOrderBy_factoryMatchesManual() {
		final OrderBy fromFactory = OrderBy.asc(
				(SerializableFunction<Model, String>) Model::getName);
		final OrderBy fromManual = new OrderBy(new OrderItem("name", Order.ASC));

		final Document factorySort = new Document();
		fromFactory.generateSort(factorySort);

		final Document manualSort = new Document();
		fromManual.generateSort(manualSort);

		Assertions.assertEquals(manualSort, factorySort);
	}
}
