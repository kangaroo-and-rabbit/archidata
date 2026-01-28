package test.atriasoft.archidata.dataAccess.model;

import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.model.GenericData;

public class ApiReadOnlyModel extends GenericData {

	@ApiReadOnly
	public String valueNotUpdate;
	public String valueUpdatable;

	@ApiReadOnly
	public Date dateNotUpdate;
	public Date dateUpdatable;

	public List<String> listUpdatable;
	@ApiReadOnly
	public List<String> listNotUpdate;

	public ApiReadOnlySubModel objectUpdatable;
	@ApiReadOnly
	public ApiReadOnlySubModel objectNotUpdate;

}
