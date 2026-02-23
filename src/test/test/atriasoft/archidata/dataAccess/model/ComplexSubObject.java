package test.atriasoft.archidata.dataAccess.model;

import java.util.List;
import java.util.Objects;

public class ComplexSubObject {

	public String name;
	public Integer count;
	public Boolean active;
	public Enum2ForTest status;
	public List<String> tags;

	public ComplexSubObject() {}

	public ComplexSubObject(final String name, final Integer count, final Boolean active, final Enum2ForTest status,
			final List<String> tags) {
		this.name = name;
		this.count = count;
		this.active = active;
		this.status = status;
		this.tags = tags;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.count, this.active, this.status, this.tags);
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
		final ComplexSubObject other = (ComplexSubObject) obj;
		return Objects.equals(this.name, other.name) && Objects.equals(this.count, other.count)
				&& Objects.equals(this.active, other.active) && this.status == other.status
				&& Objects.equals(this.tags, other.tags);
	}

}
