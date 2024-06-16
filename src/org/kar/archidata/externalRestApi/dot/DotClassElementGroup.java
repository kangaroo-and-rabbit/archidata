package org.kar.archidata.externalRestApi.dot;

import java.util.List;

import org.kar.archidata.externalRestApi.model.ClassModel;

public class DotClassElementGroup {
	private final List<DotClassElement> dotElements;
	
	public List<DotClassElement> getDotElements() {
		return this.dotElements;
	}
	
	public DotClassElementGroup(final List<DotClassElement> tsElements) {
		this.dotElements = tsElements;
	}
	
	public DotClassElement find(final ClassModel model) {
		for (final DotClassElement elem : this.dotElements) {
			if (elem.isCompatible(model)) {
				return elem;
			}
		}
		return null;
	}
	
}
