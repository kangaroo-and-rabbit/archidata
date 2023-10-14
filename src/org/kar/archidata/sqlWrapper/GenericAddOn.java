package org.kar.archidata.sqlWrapper;

import org.kar.archidata.sqlWrapper.addOn.AddOnSQLTableExternalForeinKeyAsList;
import org.kar.archidata.sqlWrapper.addOn.AddOnSQLTableExternalLink;

public class GenericAddOn {
	public static void addGenericAddOn() {
		SqlWrapper.addAddOn(new AddOnSQLTableExternalLink());
		SqlWrapper.addAddOn(new AddOnSQLTableExternalForeinKeyAsList());
	}
}
