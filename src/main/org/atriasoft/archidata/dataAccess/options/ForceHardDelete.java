package org.atriasoft.archidata.dataAccess.options;

/**
 * Option to force a real hard delete (physical removal from database) even when
 * the entity has an {@code @DataAsyncHardDeleted} field.
 *
 * <p>Without this option, calling {@code deleteHard} on an entity with
 * {@code @DataAsyncHardDeleted} will only set the hardDeleted flag to true
 * (and deleted to true), deferring the actual removal for asynchronous cleanup.
 *
 * <p>With this option, the entity is physically removed from the database immediately.
 */
public class ForceHardDelete extends QueryOption {}
