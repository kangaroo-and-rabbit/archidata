package test.kar.archidata.hybernateValidator.model;

import jakarta.validation.constraints.Size;

public class ValidatorSubModel {
	@Size(min = 2)
	public String data;
}
