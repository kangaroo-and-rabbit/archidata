package test.kar.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessSQL;
import org.kar.archidata.dataAccess.DataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.TypesTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRawQuery {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestTypes.class);

	private DataAccess da = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	public TestRawQuery() {
		this.da = DataAccess.createInterface();
		if (this.da instanceof final DataAccessSQL daSQL) {
			LOGGER.error("lkjddlkj");
		}
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(TypesTable.class);
		if (this.da instanceof final DataAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testGet() throws Exception {
		if (this.da instanceof final DataAccessSQL daSQL) {

			final TypesTable test = new TypesTable();
			test.intData = 95;
			test.floatData = 1.0F;
			this.da.insert(test);
			test.intData = 96;
			test.floatData = 2.0F;
			this.da.insert(test);
			test.intData = 97;
			test.floatData = 3.0F;
			this.da.insert(test);
			test.intData = 98;
			test.floatData = 4.0F;
			this.da.insert(test);
			test.intData = 99;
			test.floatData = 5.0F;
			this.da.insert(test);
			test.intData = 99;
			test.floatData = 6.0F;
			this.da.insert(test);
			test.intData = 99;
			test.floatData = 7.0F;
			this.da.insert(test);
			{
				final String query = """
						SELECT *
						FROM TypesTable
						WHERE `intData` = ?
						ORDER BY id DESC
						""";
				final List<Object> parameters = List.of(Integer.valueOf(99));
				// Try to retrieve all the data:
				final List<TypesTable> retrieve = daSQL.query(TypesTable.class, query, parameters);

				Assertions.assertNotNull(retrieve);
				Assertions.assertEquals(3, retrieve.size());
				Assertions.assertEquals(99, retrieve.get(0).intData);
				Assertions.assertEquals(7.0F, retrieve.get(0).floatData);
				Assertions.assertEquals(6.0F, retrieve.get(1).floatData);
				Assertions.assertEquals(5.0F, retrieve.get(2).floatData);
			}
			{

				final String query = """
						SELECT DISTINCT intData
						FROM TypesTable
						WHERE `intData` = ?
						ORDER BY id DESC
						""";
				final List<Object> parameters = List.of(Integer.valueOf(99));
				// Try to retrieve all the data:
				final List<TypesTable> retrieve = daSQL.query(TypesTable.class, query, parameters);

				Assertions.assertNotNull(retrieve);
				Assertions.assertEquals(1, retrieve.size());
				Assertions.assertEquals(99, retrieve.get(0).intData);
			}
		} else {
			LOGGER.warn("Not a SQL DB ...");
		}
	}

}
