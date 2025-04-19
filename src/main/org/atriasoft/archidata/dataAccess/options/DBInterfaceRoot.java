package org.atriasoft.archidata.dataAccess.options;

public class DBInterfaceRoot extends QueryOption {
	private final boolean root;

	public DBInterfaceRoot(final boolean root) {
		this.root = root;
	}

	public boolean getRoot() {
		return this.root;
	}

}
