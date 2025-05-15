package test.atriasoft.archidata.hybernateValidator.model;

import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public class ValidatorModel {
	@ApiReadOnly
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public String value;
	@Size(max = 5)
	public String data;

	@Valid
	public List<ValidatorSubModel> multipleElement;

	@Valid
	public ValidatorSubModel subElement;
}
