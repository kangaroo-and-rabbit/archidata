package test.atriasoft.archidata.hybernateValidator.model;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import jakarta.validation.constraints.Size;

public class ValidatorModelGroup {
	@Size(max = 5)
	public String valueNoGroup;
	@Size(max = 5, groups = GroupUpdate.class)
	public String valueUpdate;
	@Size(max = 5, groups = GroupCreate.class)
	public String valueCreate;
	@Size(max = 5, groups = { GroupCreate.class, GroupUpdate.class })
	public String valueUpdateCreate;
}
