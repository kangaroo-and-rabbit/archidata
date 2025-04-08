package sample.archidata.basic.migration;

import java.util.List;

import org.atriasoft.archidata.migration.MigrationSqlStep;
import sample.archidata.basic.model.MyModel;

public class Initialization extends MigrationSqlStep {
	public static final List<Class<?>> CLASSES_BASE = List.of(MyModel.class);
	@Override
	public String getName() {
		return "Initialization";
	}

	@Override
	public void generateStep() throws Exception {
		for(final Class<?> clazz : CLASSES_BASE) {
			addClass(clazz);
		}

		addAction("""
				ALTER TABLE `MyModel` AUTO_INCREMENT = 1000;
				""", 
				// Only MySql support this request (fail in SQLite)
				"mysql");
	}

}
