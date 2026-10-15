/**
 * Custom Bean Validation constraint annotations.
 *
 * <p>Provides validation annotations that extend the standard Jakarta Bean Validation
 * constraints with domain-specific rules, and the validation groups used to validate the same model
 * differently on creation, update, read or persistence.</p>
 *
 * <ul>
 * <li>Constraints: {@link org.atriasoft.archidata.annotation.checker.CheckForeignKey} (the
 * referenced entity exists), {@link org.atriasoft.archidata.annotation.checker.UniqueInBaseId} (the
 * value is not already used), {@link org.atriasoft.archidata.annotation.checker.ValueInList},
 * {@link org.atriasoft.archidata.annotation.checker.CollectionNotEmpty},
 * {@link org.atriasoft.archidata.annotation.checker.CollectionItemNotNull},
 * {@link org.atriasoft.archidata.annotation.checker.CollectionItemUnique}.</li>
 * <li>Groups: {@link org.atriasoft.archidata.annotation.checker.GroupCreate},
 * {@link org.atriasoft.archidata.annotation.checker.GroupUpdate},
 * {@link org.atriasoft.archidata.annotation.checker.GroupRead},
 * {@link org.atriasoft.archidata.annotation.checker.GroupPersistant}; select them on an endpoint
 * parameter with {@link org.atriasoft.archidata.annotation.checker.ValidGroup}.</li>
 * </ul>
 *
 * <p>Example:</p>
 * {@snippet lang = java :
 * public class Comment extends OIDGenericData {
 * 	@CheckForeignKey(target = Article.class)
 * 	@NotNull(groups = GroupCreate.class)
 * 	public ObjectId articleId;
 * 	@ValueInList({ "draft", "published" })
 * 	public String status;
 * }
 *
 * public Comment create(@Valid @ValidGroup(GroupCreate.class) final Comment comment) { ... }
 * }
 *
 * @see org.atriasoft.archidata.interceptor
 */
package org.atriasoft.archidata.annotation.checker;
