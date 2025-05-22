package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

public class TypeManyToOneLongRoot extends GenericData {

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneLongRemote.class)
	@Column(nullable = false)
	public Long remoteId;

	@Override
	public String toString() {
		return "TypeManyToOneRoot [otherData=" + this.otherData + ", remoteId=" + this.remoteId + ", id=" + this.id
				+ "]";
	}

}