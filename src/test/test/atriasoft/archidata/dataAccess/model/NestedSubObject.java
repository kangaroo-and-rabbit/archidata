package test.atriasoft.archidata.dataAccess.model;

import java.util.Objects;

public class NestedSubObject {

	public String label;
	public ComplexSubObject inner;

	public NestedSubObject() {}

	public NestedSubObject(final String label, final ComplexSubObject inner) {
		this.label = label;
		this.inner = inner;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.label, this.inner);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final NestedSubObject other = (NestedSubObject) obj;
		return Objects.equals(this.label, other.label) && Objects.equals(this.inner, other.inner);
	}

}
