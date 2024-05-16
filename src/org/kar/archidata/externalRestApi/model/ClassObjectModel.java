package org.kar.archidata.externalRestApi.model;

public class ClassObjectModel extends ClassModel {

	public ClassObjectModel(final Class<?> clazz) {
		this.originClasses.add(clazz);
	}
}
