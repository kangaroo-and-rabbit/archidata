package org.atriasoft.archidata.externalRestApi.python;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Manages Python import statements with deduplication.
 */
public class PyImportModel {

	public enum ImportType {
		TYPE, // Import the type itself
		CREATE, // Import the Create variant
		UPDATE // Import the Update variant
	}

	public final Map<ClassModel, Set<ImportType>> data = new HashMap<>();
	private boolean needsBaseModel = false;
	private boolean needsField = false;
	private boolean needsAnnotated = false;
	private boolean needsAny = false;

	/**
	 * Add a type import.
	 */
	public void addType(final ClassModel model) {
		add(model, ImportType.TYPE);
	}

	/**
	 * Add a create variant import.
	 */
	public void addCreate(final ClassModel model) {
		add(model, ImportType.CREATE);
	}

	/**
	 * Add an update variant import.
	 */
	public void addUpdate(final ClassModel model) {
		add(model, ImportType.UPDATE);
	}

	/**
	 * Add an import with specific type.
	 */
	public void add(final ClassModel model, final ImportType importType) {
		this.data.computeIfAbsent(model, k -> new HashSet<>()).add(importType);
	}

	/**
	 * Mark that BaseModel is needed.
	 */
	public void requestBaseModel() {
		this.needsBaseModel = true;
	}

	/**
	 * Mark that Field is needed.
	 */
	public void requestField() {
		this.needsField = true;
	}

	/**
	 * Mark that Annotated is needed.
	 */
	public void requestAnnotated() {
		this.needsAnnotated = true;
	}

	/**
	 * Mark that Any type is needed.
	 */
	public void requestAny() {
		this.needsAny = true;
	}

	public boolean hasBaseModel() {
		return this.needsBaseModel;
	}

	public boolean hasField() {
		return this.needsField;
	}

	public boolean hasAnnotated() {
		return this.needsAnnotated;
	}

	public boolean hasAny() {
		return this.needsAny;
	}

	public boolean hasImports() {
		return !this.data.isEmpty();
	}
}
