package org.atriasoft.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.tools.TypeUtils;

import jakarta.validation.Valid;

public record ParameterClassModel(
		boolean valid,
		Class<?>[] groups,
		ClassModel model) {
	public ParameterClassModel(final boolean valid, final Class<?>[] groups, final ClassModel model) {
		this.valid = valid;
		this.groups = groups;
		this.model = model;
	}

	public ParameterClassModel(final ClassModel model) {
		this(true, new Class<?>[] { GroupRead.class }, model);
	}

	public ParameterClassModel(final Valid validParam, final ValidGroup validGroupParam,
			final ClassModel parameterModel) {
		this(validParam != null, validGroupParam == null ? null : validGroupParam.value(), parameterModel);
	}

	public String getType() {
		final StringBuilder out = new StringBuilder();
		out.append(this.model.getOriginClasses().getSimpleName());
		if (!this.valid) {
			if (this.model instanceof ClassEnumModel) {
				// nothing to append
			} else {
				out.append("NV");
			}
		}
		if (this.groups == null || this.groups.length == 0) {
			return out.toString();
		}
		final List<String> groupList = new ArrayList<>();
		for (final Class<?> group : this.groups) {
			if (TypeUtils.isSameClass(group, GroupRead.class)) {
				continue;
			}
			groupList.add(group.getSimpleName().replaceAll("^Group", ""));
		}
		Collections.sort(groupList);
		for (final String group : groupList) {
			out.append(group);
		}
		return out.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ParameterClassModel that = (ParameterClassModel) o;

		// Compare groups as sets (ignores order)
		final Set<Class<?>> thisGroups = this.groups == null ? null : new HashSet<>(Arrays.asList(this.groups));
		final Set<Class<?>> thatGroups = that.groups == null ? null : new HashSet<>(Arrays.asList(that.groups));

		return this.valid == that.valid && Objects.equals(thatGroups, thisGroups);
	}

	@Override
	public int hashCode() {
		final Set<Class<?>> groupSet = this.groups == null ? null : new HashSet<>(Arrays.asList(this.groups));
		return Objects.hash(this.valid, groupSet);
	}

	@Override
	public String toString() {
		return "ParameterClassModel [valid=" + this.valid + ", groups=" + Arrays.toString(this.groups) + ", model="
				+ this.model + "]";
	}
}
