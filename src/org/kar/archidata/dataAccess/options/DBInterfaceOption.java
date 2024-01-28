package org.kar.archidata.dataAccess.options;

import java.io.IOException;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.dataAccess.QueryOption;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.db.DBConfig;
import org.kar.archidata.db.DBEntry;

public class DBInterfaceOption extends QueryOption {
	private DBEntry entry = null;
	private final DBConfig config;
	private final boolean root;
	
	public DBInterfaceOption(final DBConfig config) {
		this.config = config;
		this.root = false;
	}
	
	public DBInterfaceOption(final DBConfig config, boolean root) {
		this.config = config;
		this.root = root;
	}

	public DBEntry getEntry(QueryOptions options) throws IOException {
		if (this.entry == null) {
			final DBInterfaceRoot isRoot = options.get(DBInterfaceRoot.class);
			this.entry = DBEntry.createInterface(this.config, isRoot != null && isRoot.getRoot());
		}
		return this.entry;
	}
	
	public boolean getRoot() {
		return this.root;
	}
	
	public static DBEntry getAutoEntry(QueryOptions options) throws IOException {
		final DBInterfaceOption dbOption = options.get(DBInterfaceOption.class);
		if (dbOption == null) {
			final DBInterfaceRoot isRoot = options.get(DBInterfaceRoot.class);
			return DBEntry.createInterface(GlobalConfiguration.dbConfig, isRoot != null && isRoot.getRoot());
		} else {
			return dbOption.getEntry(options);
		}
	}
	
}
