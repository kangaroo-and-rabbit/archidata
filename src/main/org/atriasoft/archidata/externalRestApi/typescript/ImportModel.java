package org.atriasoft.archidata.externalRestApi.typescript;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages TypeScript import statements with deduplication and mode tracking.
 */
public class ImportModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportModel.class);

	/** Defines the mode/category of import to generate. */
	public enum ModeImport {
		/** Import the Zod schema. */
		ZOD,
		/** Import the TypeScript type. */
		TYPE,
		/** Import the type-check function. */
		IS
	}

	/**
	 * Represents an import entry with its mode and validation context.
	 * @param mode the import mode (ZOD, TYPE, or IS)
	 * @param valid whether validation is active
	 * @param groups the validation groups to consider
	 */
	public record PairElem(
			ModeImport mode,
			boolean valid,
			Class<?>[] groups) {

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final PairElem that = (PairElem) o;

			// Compare groups as sets (ignores order)
			final Set<Class<?>> thisGroups = this.groups == null ? null : new HashSet<>(Arrays.asList(this.groups));
			final Set<Class<?>> thatGroups = that.groups == null ? null : new HashSet<>(Arrays.asList(that.groups));

			return this.valid == that.valid && Objects.equals(thisGroups, thatGroups) && this.mode == that.mode;
		}

		@Override
		public int hashCode() {
			final Set<Class<?>> groupSet = this.groups == null ? null : new HashSet<>(Arrays.asList(this.groups));
			return Objects.hash(this.valid, groupSet);
		}
	}

	/** Map of class models to their required import entries. */
	final Map<ClassModel, Set<PairElem>> data = new HashMap<>();

	/**
	 * Adds a TYPE import for the given model with validation context.
	 * @param valid whether validation is active
	 * @param groups the validation groups
	 * @param model the class model to import
	 */
	public void add(final boolean valid, final Class<?>[] groups, final ClassModel model) {
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		values.add(new PairElem(ModeImport.TYPE, valid, groups));
		this.data.put(model, values);
	}

	/**
	 * Adds a TYPE import for the given model with default GroupRead validation.
	 * @param model the class model to import
	 */
	public void add(final ClassModel model) {
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		values.add(new PairElem(ModeImport.TYPE, true, new Class<?>[] { GroupRead.class }));
		this.data.put(model, values);
	}

	/**
	 * Adds a ZOD schema import for the given model with validation context.
	 * @param valid whether validation is active
	 * @param groups the validation groups
	 * @param model the class model to import
	 */
	public void addZod(final boolean valid, final Class<?>[] groups, final ClassModel model) {
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		values.add(new PairElem(ModeImport.ZOD, valid, groups));
		this.data.put(model, values);
	}

	/**
	 * Adds a ZOD schema import for the given model with default GroupRead validation.
	 * @param model the class model to import
	 */
	public void addZod(final ClassModel model) {
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		values.add(new PairElem(ModeImport.ZOD, true, new Class<?>[] { GroupRead.class }));
		this.data.put(model, values);
	}

	/**
	 * Adds a type-check (IS) import for the given model with validation context.
	 * @param valid whether validation is active
	 * @param groups the validation groups
	 * @param model the class model to import
	 */
	public void addCheck(final boolean valid, final Class<?>[] groups, final ClassModel model) {
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		values.add(new PairElem(ModeImport.IS, valid, groups));
		this.data.put(model, values);
	}

	/**
	 * Adds a type-check (IS) import for the given model with default GroupRead validation.
	 * @param model the class model to import
	 */
	public void addCheck(final ClassModel model) {
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		values.add(new PairElem(ModeImport.IS, true, new Class<?>[] { GroupRead.class }));
		this.data.put(model, values);
	}

	/** Whether a direct Zod library import is needed. */
	boolean hasZodImport = false;

	/**
	 * Checks whether a direct Zod library import is required.
	 * @return true if a Zod import has been requested
	 */
	public boolean hasZodImport() {
		return this.hasZodImport;
	}

	/**
	 * Marks that a direct Zod library import is needed.
	 */
	public void requestZod() {
		this.hasZodImport = true;
	}

}