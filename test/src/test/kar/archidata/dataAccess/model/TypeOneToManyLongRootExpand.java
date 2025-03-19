package test.kar.archidata.dataAccess.model;

import java.util.List;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyLongRoot")
//for Mongo
@Entity(value = "TypeOneToManyLongRoot")
public class TypeOneToManyLongRootExpand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyLongRemote.class, mappedBy = "rootId")
	@Column(nullable = false)
	public List<TypeOneToManyLongRemote> remotes;
}
