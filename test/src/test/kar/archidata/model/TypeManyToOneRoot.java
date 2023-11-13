package test.kar.archidata.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

public class TypeManyToOneRoot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneRemote.class)
	@Column(nullable = false)
	public Long remoteId;
}