package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyNoSqlOIDRoot")
// for Mongo
@Entity(value = "TypeManyToManyNoSqlOIDRoot")
public class TypeManyToManyNoSqlOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToManyNoSQL(targetEntity = TypeManyToManyNoSqlOIDRemote.class, remoteField = "remoteToParent")
	public List<TypeManyToManyNoSqlOIDRemote> remote;
}
