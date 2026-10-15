package sample.archidata.basic.model;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A to-do item.
 *
 * <p>Inherited from {@link GenericDataSoftDelete}:
 * <ul>
 * <li>{@code id} (Long, generated on insert, stored as {@code _id});</li>
 * <li>{@code createdAt} / {@code updatedAt} (filled by the data access layer);</li>
 * <li>{@code deleted} (soft delete: {@code DataAccess.deleteById} only sets this flag, and the
 * default reads skip the deleted documents).</li>
 * </ul>
 * The parent classes also carry the group constraints ({@code GroupCreate}, {@code GroupUpdate}) that
 * reject a payload setting one of these server-managed fields.
 */
// Stored in the "Task" collection (the class name; use @Table(name = "...") to change it).
@Entity
// Omit null fields in the JSON output.
@JsonInclude(JsonInclude.Include.NON_NULL)
// Compound MongoDB index for "open tasks, newest first". It is created by IndexEngine at startup
// (see WebLauncher), not by the migrations.
@Index({ "done", "-createdAt" })
public class Task extends GenericDataSoftDelete {
	// Bean Validation constraints of the Default group: checked by Jersey when the parameter is @Valid.
	@NotNull
	@Size(min = 1, max = 256)
	@Schema(description = "Short title of the task")
	private String title;

	// length = 0: no length limit at the storage level; @Size bounds what the API accepts.
	@Column(length = 0)
	@Size(max = 4096)
	@Schema(description = "Free text description")
	private String description;

	@NotNull
	@Schema(description = "True when the task is finished")
	private Boolean done;

	public Task() {}

	public Task(final String title, final String description) {
		this.title = title;
		this.description = description;
		this.done = Boolean.FALSE;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Boolean getDone() {
		return this.done;
	}

	public void setDone(final Boolean done) {
		this.done = done;
	}

	@Override
	public String toString() {
		return "Task [id=" + getId() + ", title=" + this.title + ", done=" + this.done + "]";
	}
}
