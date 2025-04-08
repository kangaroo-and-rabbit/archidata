package sample.archidata.basic.model;

import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.Data;
import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// needed for Swagger interface
@Entity
// Do not generate Null in Json serialization ==> prefer undefined
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyModel extends GenericDataSoftDelete {
	// Can not be NULL and max length is 300 (note default is 255)
	@Column(nullable = false, length = 300)
	public String name;
	// Can be null and no max length
	@Column(length = 0)
	public String description;

	@Override
	public String toString() {
		return "MyModel [name=" + this.name + ", description=" + this.description + "]";
	}


}
