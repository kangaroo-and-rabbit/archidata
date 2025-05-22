package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyNoSqlOIDRoot")
public class TypeOneToManyNoSqlOIDRootExpand extends OIDGenericData {

	public String otherData;

	@OneToManyNoSQL(targetEntity = TypeOneToManyNoSqlOIDRemote.class, remoteField = "rootOid")
	@Column(nullable = false, name = "remoteIds")
	public List<TypeOneToManyNoSqlOIDRemote> remotes;
}
