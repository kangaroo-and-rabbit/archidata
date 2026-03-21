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

	/** Defines the type of import to generate. */
	public enum ImportType {
		/** Import the type itself. */
		TYPE,
		/** Import the Create variant. */
		CREATE,
		/** Import the Update variant. */
		UPDATE
	}

	/** Map of class models to their required import types. */
	public final Map<ClassModel, Set<ImportType>> data = new HashMap<>();
	private boolean needsBaseModel = false;
	private boolean needsField = false;
	private boolean needsAnnotated = false;
	private boolean needsAny = false;

	/**
	 * Adds a type import for the given model.
	 * @param model the class model to import
	 */
	public void addType(final ClassModel model) {
		add(model, ImportType.TYPE);
	}

	/**
	 * Adds a Create variant import for the given model.
	 * @param model the class model to import as Create variant
	 */
	public void addCreate(final ClassModel model) {
		add(model, ImportType.CREATE);
	}

	/**
	 * Adds an Update variant import for the given model.
	 * @param model the class model to import as Update variant
	 */
	public void addUpdate(final ClassModel model) {
		add(model, ImportType.UPDATE);
	}

	/**
	 * Adds an import with the specified import type.
	 * @param model the class model to import
	 * @param importType the type of import to add
	 */
	public void add(final ClassModel model, final ImportType importType) {
		this.data.computeIfAbsent(model, k -> new HashSet<>()).add(importType);
	}

	/**
	 * Marks that the Pydantic BaseModel import is needed.
	 */
	public void requestBaseModel() {
		this.needsBaseModel = true;
	}

	/**
	 * Marks that the Pydantic Field import is needed.
	 */
	public void requestField() {
		this.needsField = true;
	}

	/**
	 * Marks that the typing.Annotated import is needed.
	 */
	public void requestAnnotated() {
		this.needsAnnotated = true;
	}

	/**
	 * Marks that the typing.Any import is needed.
	 */
	public void requestAny() {
		this.needsAny = true;
	}

	/**
	 * Checks whether the BaseModel import is required.
	 * @return true if BaseModel has been requested
	 */
	public boolean hasBaseModel() {
		return this.needsBaseModel;
	}

	/**
	 * Checks whether the Field import is required.
	 * @return true if Field has been requested
	 */
	public boolean hasField() {
		return this.needsField;
	}

	/**
	 * Checks whether the Annotated import is required.
	 * @return true if Annotated has been requested
	 */
	public boolean hasAnnotated() {
		return this.needsAnnotated;
	}

	/**
	 * Checks whether the Any type import is required.
	 * @return true if Any has been requested
	 */
	public boolean hasAny() {
		return this.needsAny;
	}

	/**
	 * Checks whether any model imports have been registered.
	 * @return true if there are model imports
	 */
	public boolean hasImports() {
		return !this.data.isEmpty();
	}
}
