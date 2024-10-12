package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneRoot")
public class TypeManyToOneRootExpand extends GenericData {

	public String otherData;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeManyToOneRemote.class)
	@Column(name = "remoteId", nullable = false)
	public TypeManyToOneRemote remote;

	@Override
	public String toString() {
		return "TypeManyToOneRootExpand [otherData=" + this.otherData + ", remote=" + this.remote + ", id=" + this.id
				+ "]";
	}

}