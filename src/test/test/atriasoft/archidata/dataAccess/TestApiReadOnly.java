package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.ApiReadOnlyModel;
import test.atriasoft.archidata.dataAccess.model.ApiReadOnlySubModel;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestApiReadOnly {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestApiReadOnly.class);

	private static final String VALUE_UPDATABLE_INITIAL = "updatable_initial";
	private static final String VALUE_NOT_UPDATE_INITIAL = "readonly_initial";
	private static final String VALUE_UPDATABLE_MODIFIED = "updatable_modified";
	private static final String VALUE_NOT_UPDATE_MODIFIED = "readonly_modified";

	private static final Date DATE_UPDATABLE_INITIAL = new Date(1000000000L);
	private static final Date DATE_NOT_UPDATE_INITIAL = new Date(2000000000L);
	private static final Date DATE_UPDATABLE_MODIFIED = new Date(3000000000L);
	private static final Date DATE_NOT_UPDATE_MODIFIED = new Date(4000000000L);

	private static final List<String> LIST_UPDATABLE_INITIAL = List.of("a", "b");
	private static final List<String> LIST_NOT_UPDATE_INITIAL = List.of("x", "y");
	private static final List<String> LIST_UPDATABLE_MODIFIED = List.of("c", "d");
	private static final List<String> LIST_NOT_UPDATE_MODIFIED = List.of("z", "w");

	private static Long idOfTheObject = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		idOfTheObject = null;
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	private static ApiReadOnlySubModel createSubModel(final String data) {
		final ApiReadOnlySubModel sub = new ApiReadOnlySubModel();
		sub.data = data;
		return sub;
	}

	@Order(1)
	@Test
	public void testInsertAndRetrieve() throws Exception {
		final ApiReadOnlyModel test = new ApiReadOnlyModel();
		test.valueUpdatable = VALUE_UPDATABLE_INITIAL;
		test.valueNotUpdate = VALUE_NOT_UPDATE_INITIAL;
		test.dateUpdatable = DATE_UPDATABLE_INITIAL;
		test.dateNotUpdate = DATE_NOT_UPDATE_INITIAL;
		test.listUpdatable = LIST_UPDATABLE_INITIAL;
		test.listNotUpdate = LIST_NOT_UPDATE_INITIAL;
		test.objectUpdatable = createSubModel("obj_updatable");
		test.objectNotUpdate = createSubModel("obj_readonly");

		final ApiReadOnlyModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Retrieve and verify all fields were written correctly
		final ApiReadOnlyModel retrieve = ConfigureDb.da.getById(ApiReadOnlyModel.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertEquals(VALUE_UPDATABLE_INITIAL, retrieve.valueUpdatable);
		Assertions.assertEquals(VALUE_NOT_UPDATE_INITIAL, retrieve.valueNotUpdate);
		Assertions.assertEquals(DATE_UPDATABLE_INITIAL, retrieve.dateUpdatable);
		Assertions.assertEquals(DATE_NOT_UPDATE_INITIAL, retrieve.dateNotUpdate);
		Assertions.assertEquals(LIST_UPDATABLE_INITIAL, retrieve.listUpdatable);
		Assertions.assertEquals(LIST_NOT_UPDATE_INITIAL, retrieve.listNotUpdate);
		Assertions.assertNotNull(retrieve.objectUpdatable);
		Assertions.assertEquals("obj_updatable", retrieve.objectUpdatable.data);
		Assertions.assertNotNull(retrieve.objectNotUpdate);
		Assertions.assertEquals("obj_readonly", retrieve.objectNotUpdate.data);

		idOfTheObject = retrieve.id;
	}

	@Order(2)
	@Test
	public void testUpdateOnlyUpdatableFields() throws Exception {
		// Attempt to update all fields but only specify updatable ones in FilterValue
		final ApiReadOnlyModel updateData = new ApiReadOnlyModel();
		updateData.valueUpdatable = VALUE_UPDATABLE_MODIFIED;
		updateData.valueNotUpdate = VALUE_NOT_UPDATE_MODIFIED;
		updateData.dateUpdatable = DATE_UPDATABLE_MODIFIED;
		updateData.dateNotUpdate = DATE_NOT_UPDATE_MODIFIED;
		updateData.listUpdatable = LIST_UPDATABLE_MODIFIED;
		updateData.listNotUpdate = LIST_NOT_UPDATE_MODIFIED;
		updateData.objectUpdatable = createSubModel("obj_updatable_modified");
		updateData.objectNotUpdate = createSubModel("obj_readonly_modified");

		// Update specifying only the updatable fields via FilterValue
		ConfigureDb.da.updateById(updateData, idOfTheObject,
				new FilterValue("valueUpdatable", "dateUpdatable", "listUpdatable", "objectUpdatable"));

		final ApiReadOnlyModel retrieve = ConfigureDb.da.getById(ApiReadOnlyModel.class, idOfTheObject);

		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(idOfTheObject, retrieve.id);

		// Updatable fields should be modified
		Assertions.assertEquals(VALUE_UPDATABLE_MODIFIED, retrieve.valueUpdatable);
		Assertions.assertEquals(DATE_UPDATABLE_MODIFIED, retrieve.dateUpdatable);
		Assertions.assertEquals(LIST_UPDATABLE_MODIFIED, retrieve.listUpdatable);
		Assertions.assertNotNull(retrieve.objectUpdatable);
		Assertions.assertEquals("obj_updatable_modified", retrieve.objectUpdatable.data);

		// @ApiReadOnly fields should NOT be modified
		Assertions.assertEquals(VALUE_NOT_UPDATE_INITIAL, retrieve.valueNotUpdate);
		Assertions.assertEquals(DATE_NOT_UPDATE_INITIAL, retrieve.dateNotUpdate);
		Assertions.assertEquals(LIST_NOT_UPDATE_INITIAL, retrieve.listNotUpdate);
		Assertions.assertNotNull(retrieve.objectNotUpdate);
		Assertions.assertEquals("obj_readonly", retrieve.objectNotUpdate.data);
	}
}
