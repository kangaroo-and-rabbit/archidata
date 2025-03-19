package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

@Entity
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