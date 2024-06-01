package org.kar.archidata.dataAccess.options;

import java.io.IOException;
import java.util.List;

import org.kar.archidata.GlobalConfiguration;
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

	public DBInterfaceOption(final DBConfig config, final boolean root) {
		this.config = config;
		this.root = root;
	}

	public DBEntry getEntry(final QueryOptions options) throws IOException {
		if (this.entry == null) {
			final List<DBInterfaceRoot> isRoot = options.get(DBInterfaceRoot.class);
			this.entry = DBEntry.createInterface(this.config, isRoot.size() == 1 && isRoot.get(0).getRoot());
		}
		return this.entry;
	}

	public boolean getRoot() {
		return this.root;
	}

	public static DBEntry getAutoEntry(final QueryOptions options) throws IOException {
		if (options == null) {
			return DBEntry.createInterface(GlobalConfiguration.dbConfig, false);
		}
		final List<DBInterfaceOption> dbOption = options.get(DBInterfaceOption.class);
		if (dbOption.size() == 0) {
			final List<DBInterfaceRoot> isRoot = options.get(DBInterfaceRoot.class);
			return DBEntry.createInterface(GlobalConfiguration.dbConfig, isRoot.size() == 1 && isRoot.get(0).getRoot());
		} else {
			return dbOption.get(0).getEntry(options);
		}
	}

}
