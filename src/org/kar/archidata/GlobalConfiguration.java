package org.kar.archidata;

import org.kar.archidata.db.DBConfig;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ConfigBaseVariable;

public class GlobalConfiguration {
	private static final DBConfig dbConfig;

	static {
		DBConfig dbConfigTmp = null;
		try {
			dbConfigTmp = new DBConfig(ConfigBaseVariable.getDBType(), ConfigBaseVariable.getDBHost(),
					Integer.parseInt(ConfigBaseVariable.getDBPort()), ConfigBaseVariable.getDBLogin(),
					ConfigBaseVariable.getDBPassword(), ConfigBaseVariable.getDBName(),
					ConfigBaseVariable.getDBKeepConnected());
		} catch (NumberFormatException | DataAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Fail to configure db access ... in static GlobalConfiguration");
		}
		dbConfig = dbConfigTmp;
	}

	public static DBConfig getDbconfig() {
		return dbConfig;
	}
}
