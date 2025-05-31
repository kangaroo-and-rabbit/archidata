package test.atriasoft.archidata.dataAccess.model;

import java.util.Objects;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DataInJson {

	// Simple data to verify if the checker is active
	@Size(min = 3, max = 128)
	@Pattern(regexp = "^[0-9]+$")
	public String data;

	public DataInJson(String data) {
		this.data = data;
	}

	public DataInJson() {}

	@Override
	public int hashCode() {
		return Objects.hash(data);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataInJson other = (DataInJson) obj;
		return Objects.equals(data, other.data);
	}

}
