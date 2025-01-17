package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeManyToOneRoot extends GenericData {

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneRemote.class)
	@Column(nullable = false)
	public Long remoteId;

	@Override
	public String toString() {
		return "TypeManyToOneRoot [otherData=" + this.otherData + ", remoteId=" + this.remoteId + ", id=" + this.id
				+ "]";
	}

}