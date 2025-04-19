package test.atriasoft.archidata.dataAccess.model;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class TypesEnum2 {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;

	public Enum2ForTest data;
}