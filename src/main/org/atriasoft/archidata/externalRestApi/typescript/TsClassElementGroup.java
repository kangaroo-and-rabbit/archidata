package org.atriasoft.archidata.externalRestApi.typescript;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

public class TsClassElementGroup {
	private final List<TsClassElement> tsElements;

	public List<TsClassElement> getTsElements() {
		return this.tsElements;
	}

	public TsClassElementGroup(final List<TsClassElement> tsElements) {
		this.tsElements = tsElements;
	}

	public TsClassElement find(final ClassModel model) {
		for (final TsClassElement elem : this.tsElements) {
			if (elem.isCompatible(model)) {
				return elem;
			}
		}
		return null;
	}

}
