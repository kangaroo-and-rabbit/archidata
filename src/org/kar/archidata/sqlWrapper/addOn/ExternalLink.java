package org.kar.archidata.sqlWrapper.addOn;

import java.lang.reflect.Field;

import org.kar.archidata.annotation.SQLTableLinkGeneric;
import org.kar.archidata.sqlWrapper.SqlWrapperAddOn;

public class ExternalLink implements SqlWrapperAddOn {

	@Override
	public Class<?> getAnnotationClass() {
		return SQLTableLinkGeneric.class;
	}
	public String getSQLFieldType(Field elem) {
		return "STRING";
	}
	public boolean isCompatibleField(Field elem) {
		SQLTableLinkGeneric decorators = elem.getDeclaredAnnotation(SQLTableLinkGeneric.class);
		return decorators != null;
	}
}
