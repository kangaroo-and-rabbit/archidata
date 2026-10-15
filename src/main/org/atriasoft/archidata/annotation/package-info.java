/**
 * Annotations for data model definition, validation constraints, and API behavior.
 *
 * <p>This package provides annotations used to configure how model classes are persisted,
 * validated, and exposed through REST APIs, together with the standard Bean Validation
 * annotations. archidata does not read the JPA annotations ({@code jakarta.persistence}).</p>
 *
 * <ul>
 * <li>Mapping: {@link org.atriasoft.archidata.annotation.Collection} (collection name),
 * {@link org.atriasoft.archidata.annotation.Field} (field name in the database),
 * {@link org.atriasoft.archidata.annotation.Id} (primary key),
 * {@link org.atriasoft.archidata.annotation.InitValue} (value written at insertion when
 * {@code null}).</li>
 * <li>Persistence behavior: {@link org.atriasoft.archidata.annotation.CreationTimestamp},
 * {@link org.atriasoft.archidata.annotation.UpdateTimestamp},
 * {@link org.atriasoft.archidata.annotation.DataDeleted} (soft delete),
 * {@link org.atriasoft.archidata.annotation.DataNotRead},
 * {@link org.atriasoft.archidata.annotation.DataEncrypt}.</li>
 * <li>Relations: {@link org.atriasoft.archidata.annotation.ManyToOneDoc},
 * {@link org.atriasoft.archidata.annotation.OneToManyDoc},
 * {@link org.atriasoft.archidata.annotation.ManyToManyDoc}.</li>
 * <li>MongoDB indexes: {@link org.atriasoft.archidata.annotation.Index} (class level, repeatable
 * through {@link org.atriasoft.archidata.annotation.IndexList}) and
 * {@link org.atriasoft.archidata.annotation.Indexed} (property level).</li>
 * <li>Reflection helper: {@link org.atriasoft.archidata.annotation.AnnotationTools}.</li>
 * </ul>
 *
 * <p>Example:</p>
 * {@snippet lang = java :
 * @Collection("article")
 * public class Article extends OIDGenericDataSoftDelete {
 * 	@Indexed
 * 	@NotNull
 * 	@Size(max = 200)
 * 	public String title;
 * 	@DataNotRead
 * 	public String internalNote;
 * 	@CheckForeignKey(target = User.class)
 * 	public ObjectId authorId;
 * }
 * }
 *
 * <p>See <code>doc/data_model.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/data_model.md">online</a>).</p>
 *
 * @see org.atriasoft.archidata.annotation.checker
 * @see org.atriasoft.archidata.annotation.apiGenerator
 * @see org.atriasoft.archidata.annotation.security
 */
package org.atriasoft.archidata.annotation;
