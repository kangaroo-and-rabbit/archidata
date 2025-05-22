package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneLongRoot")
public class TypeManyToOneLongRootExpand extends GenericData {

	public String otherData;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeManyToOneLongRemote.class)
	@Column(name = "remoteId", nullable = false)
	public TypeManyToOneLongRemote remote;

	@Override
	public String toString() {
		return "TypeManyToOneRootExpand [otherData=" + this.otherData + ", remote=" + this.remote + ", id=" + this.id
				+ "]";
	}

}