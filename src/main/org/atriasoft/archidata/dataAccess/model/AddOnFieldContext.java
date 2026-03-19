package org.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Pre-computed metadata for an AddOn-managed field. Built once during
 * {@link DbClassModel} construction and reused at every CRUD operation,
 * eliminating repeated annotation lookups, {@code DbClassModel.of()} calls,
 * and linear field searches.
 */
public final class AddOnFieldContext {

	// --- Common to all relation types ---
	private final Class<?> targetEntity;
	private final String remoteField;
	private final String remoteFieldColumn;
	private final DbPropertyDescriptor targetPk;
	private final String targetPkColumn;
	private final Class<?> targetPkType;
	private final boolean entityReference;

	// --- ManyToOne specific ---
	private final boolean addLinkWhenCreate;
	private final boolean removeLinkWhenDelete;
	private final boolean updateLinkWhenUpdate;

	// --- OneToMany specific ---
	private final CascadeMode cascadeUpdate;
	private final CascadeMode cascadeDelete;

	private AddOnFieldContext(final Builder builder) {
		this.targetEntity = builder.targetEntity;
		this.remoteField = builder.remoteField;
		this.remoteFieldColumn = builder.remoteFieldColumn;
		this.targetPk = builder.targetPk;
		this.targetPkColumn = builder.targetPkColumn;
		this.targetPkType = builder.targetPkType;
		this.entityReference = builder.entityReference;
		this.addLinkWhenCreate = builder.addLinkWhenCreate;
		this.removeLinkWhenDelete = builder.removeLinkWhenDelete;
		this.updateLinkWhenUpdate = builder.updateLinkWhenUpdate;
		this.cascadeUpdate = builder.cascadeUpdate;
		this.cascadeDelete = builder.cascadeDelete;
	}

	// ========== Getters ==========

	public Class<?> getTargetEntity() {
		return this.targetEntity;
	}

	public String getRemoteField() {
		return this.remoteField;
	}

	/** The DB column name of the remote field (pre-resolved). */
	public String getRemoteFieldColumn() {
		return this.remoteFieldColumn;
	}

	/** The primary key descriptor of the target entity. */
	public DbPropertyDescriptor getTargetPk() {
		return this.targetPk;
	}

	/** The DB column name of the target entity's primary key (without QueryOptions rename). */
	public String getTargetPkColumn() {
		return this.targetPkColumn;
	}

	/** The Java type of the target entity's primary key (Long, UUID, ObjectId). */
	public Class<?> getTargetPkType() {
		return this.targetPkType;
	}

	/** True if the field stores full entity objects, false if it stores raw IDs. */
	public boolean isEntityReference() {
		return this.entityReference;
	}

	public boolean isAddLinkWhenCreate() {
		return this.addLinkWhenCreate;
	}

	public boolean isRemoveLinkWhenDelete() {
		return this.removeLinkWhenDelete;
	}

	public boolean isUpdateLinkWhenUpdate() {
		return this.updateLinkWhenUpdate;
	}

	public CascadeMode getCascadeUpdate() {
		return this.cascadeUpdate;
	}

	public CascadeMode getCascadeDelete() {
		return this.cascadeDelete;
	}

	// ========== Factory methods ==========

	static AddOnFieldContext buildFor(final DbPropertyDescriptor desc) throws IntrospectionException {
		final PropertyDescriptor prop = desc.getProperty();

		final ManyToManyDoc m2m = prop.getAnnotation(ManyToManyDoc.class);
		if (m2m != null) {
			return buildManyToMany(desc, m2m);
		}
		final ManyToOneDoc m2o = prop.getAnnotation(ManyToOneDoc.class);
		if (m2o != null) {
			return buildManyToOne(desc, m2o);
		}
		final OneToManyDoc o2m = prop.getAnnotation(OneToManyDoc.class);
		if (o2m != null) {
			return buildOneToMany(desc, o2m);
		}
		return null;
	}

	private static AddOnFieldContext buildManyToMany(final DbPropertyDescriptor desc, final ManyToManyDoc ann)
			throws IntrospectionException {
		final Builder b = new Builder();
		b.targetEntity = ann.targetEntity();
		b.remoteField = ann.remoteField();
		b.addLinkWhenCreate = true;
		b.removeLinkWhenDelete = false;
		b.updateLinkWhenUpdate = false;
		b.cascadeUpdate = CascadeMode.IGNORE;
		b.cascadeDelete = CascadeMode.IGNORE;
		resolveTarget(b, desc);
		return new AddOnFieldContext(b);
	}

	private static AddOnFieldContext buildManyToOne(final DbPropertyDescriptor desc, final ManyToOneDoc ann)
			throws IntrospectionException {
		final Builder b = new Builder();
		b.targetEntity = ann.targetEntity();
		b.remoteField = ann.remoteField();
		b.addLinkWhenCreate = ann.addLinkWhenCreate();
		b.removeLinkWhenDelete = ann.removeLinkWhenDelete();
		b.updateLinkWhenUpdate = ann.updateLinkWhenUpdate();
		b.cascadeUpdate = CascadeMode.IGNORE;
		b.cascadeDelete = CascadeMode.IGNORE;
		resolveTarget(b, desc);
		return new AddOnFieldContext(b);
	}

	private static AddOnFieldContext buildOneToMany(final DbPropertyDescriptor desc, final OneToManyDoc ann)
			throws IntrospectionException {
		final Builder b = new Builder();
		b.targetEntity = ann.targetEntity();
		b.remoteField = ann.remoteField();
		b.addLinkWhenCreate = ann.addLinkWhenCreate();
		b.removeLinkWhenDelete = false;
		b.updateLinkWhenUpdate = false;
		b.cascadeUpdate = ann.cascadeUpdate();
		b.cascadeDelete = ann.cascadeDelete();
		resolveTarget(b, desc);
		return new AddOnFieldContext(b);
	}

	private static void resolveTarget(final Builder b, final DbPropertyDescriptor desc) throws IntrospectionException {
		final DbClassModel targetModel = DbClassModel.of(b.targetEntity);
		b.targetPk = targetModel.getPrimaryKey();
		if (b.targetPk != null) {
			b.targetPkColumn = b.targetPk.getFieldName(null).inTable();
			b.targetPkType = b.targetPk.getProperty().getType();
		}
		// Resolve remote field column (if remoteField is specified)
		if (b.remoteField != null && !b.remoteField.isEmpty()) {
			final DbPropertyDescriptor remoteDesc = targetModel.findByPropertyName(b.remoteField);
			if (remoteDesc == null) {
				throw new IntrospectionException(
						"Cannot find remote field '" + b.remoteField + "' in " + b.targetEntity.getSimpleName());
			}
			b.remoteFieldColumn = remoteDesc.getFieldName(null).inTable();
		}
		// Determine if field stores entity objects vs raw IDs
		final PropertyDescriptor prop = desc.getProperty();
		if (java.util.Collection.class.isAssignableFrom(prop.getType())) {
			b.entityReference = (prop.getElementType() == b.targetEntity);
		} else {
			b.entityReference = (prop.getType() == b.targetEntity);
		}
	}

	// ========== Builder ==========

	private static final class Builder {
		Class<?> targetEntity;
		String remoteField;
		String remoteFieldColumn;
		DbPropertyDescriptor targetPk;
		String targetPkColumn;
		Class<?> targetPkType;
		boolean entityReference;
		boolean addLinkWhenCreate;
		boolean removeLinkWhenDelete;
		boolean updateLinkWhenUpdate;
		CascadeMode cascadeUpdate;
		CascadeMode cascadeDelete;
	}
}
