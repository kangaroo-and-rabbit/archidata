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

public class ImportModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportModel.class);

	public enum ModeImport {
		ZOD, TYPE, IS
	}

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

	final Map<ClassModel, Set<PairElem>> data = new HashMap<>();

	public void add(final boolean valid, final Class<?>[] groups, final ClassModel model) {
		if (model.getOriginClasses() != null && "BaseEntity".equals(model.getOriginClasses().getSimpleName())) {
			LOGGER.error("lkjlkjlkj");
		}
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		LOGGER.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Add import: {}",
				model.getOriginClasses() == null ? null : model.getOriginClasses().getSimpleName());
		values.add(new PairElem(ModeImport.TYPE, valid, groups));
		this.data.put(model, values);
	}

	public void add(final ClassModel model) {
		if (model.getOriginClasses() != null && "BaseEntity".equals(model.getOriginClasses().getSimpleName())) {
			LOGGER.error("lkjlkjlkj");
		}
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		LOGGER.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Add import: {}",
				model.getOriginClasses() == null ? null : model.getOriginClasses().getSimpleName());
		values.add(new PairElem(ModeImport.TYPE, true, new Class<?>[] { GroupRead.class }));
		this.data.put(model, values);
	}

	public void addZod(final ClassModel model) {
		if (model.getOriginClasses() != null && "BaseEntity".equals(model.getOriginClasses().getSimpleName())) {
			LOGGER.error("lkjlkjlkj");
		}
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		LOGGER.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Add import ZOD : {}",
				model.getOriginClasses() == null ? null : model.getOriginClasses().getSimpleName());
		values.add(new PairElem(ModeImport.ZOD, true, new Class<?>[] { GroupRead.class }));
		this.data.put(model, values);
	}

	public void addCheck(final ClassModel model) {
		if (model.getOriginClasses() != null && "BaseEntity".equals(model.getOriginClasses().getSimpleName())) {
			LOGGER.error("lkjlkjlkj");
		}
		final Set<PairElem> values = this.data.getOrDefault(model, new HashSet<>());
		LOGGER.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Add import CHECK: {}",
				model.getOriginClasses() == null ? null : model.getOriginClasses().getSimpleName());
		values.add(new PairElem(ModeImport.IS, true, new Class<?>[] { GroupRead.class }));
		this.data.put(model, values);
	}

	boolean hasZodImport = false;

	public boolean hasZodImport() {
		return this.hasZodImport;
	}

	public void requestZod() {
		this.hasZodImport = true;
	}

}