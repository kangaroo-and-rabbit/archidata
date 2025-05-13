package test.atriasoft.archidata.hybernateValidator.model;

import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public class ValidatorModel {
	@ApiReadOnly
	public String value;
	@Size(max = 5)
	public String data;

	@Valid
	public List<ValidatorSubModel> multipleElement;

	@Valid
	public ValidatorSubModel subElement;
}
