package test.kar.archidata.dataAccess.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyRoot")
public class TypeOneToManyRootExpand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyRemote.class, mappedBy = "rootId")
	@Column(nullable = false)
	public List<TypeOneToManyRemote> remotes;
}
