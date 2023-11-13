package org.kar.archidata.backup;

import java.util.ArrayList;
import java.util.List;

public class BackupEngine {

	public enum StoreMode {
		JSON, SQL
	}

	private final String pathStore;
	private final StoreMode mode;
	private final List<Class<?>> classes = new ArrayList<>();

	public BackupEngine(final String pathToStoreDB, final StoreMode mode) {
		this.pathStore = pathToStoreDB;
		this.mode = mode;
	}

	public void addClass(final Class<?> clazz) {
		this.classes.add(clazz);
	}

	public void store() {
		// TODO ...

	}
}
