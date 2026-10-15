/**
 * MongoDB data access layer providing CRUD operations, queries, and transactions.
 *
 * <p>The central class {@link org.atriasoft.archidata.dataAccess.DBAccessMongo} handles
 * all database interactions including inserts, updates, deletes, and queries with
 * support for conditions, ordering, pagination, and transactions.</p>
 *
 * <p>Entry points:</p>
 * <ul>
 * <li>{@link org.atriasoft.archidata.dataAccess.DataAccess}: static facade, the usual way to
 * call the database ({@code insert}, {@code get}, {@code gets}, {@code getById}, {@code update},
 * {@code updateById}, {@code deleteById}, {@code count}, {@code existsById}, {@code inTransaction}...).
 * It uses the connection of the current thread (see {@link org.atriasoft.archidata.checker.DataAccessConnectionContext}).</li>
 * <li>{@link org.atriasoft.archidata.dataAccess.DBAccessMongo}: the instance behind it, created with
 * {@link org.atriasoft.archidata.dataAccess.DBAccessMongo#createInterface()}; must be closed.</li>
 * <li>{@link org.atriasoft.archidata.dataAccess.Filters}: type-safe MongoDB filters
 * ({@code eq}, {@code gt}, {@code in}, {@code and}...), by field name or by getter reference.</li>
 * <li>{@link org.atriasoft.archidata.dataAccess.TransactionContext}: explicit MongoDB transaction.</li>
 * </ul>
 *
 * <p>Every call takes a list of {@link org.atriasoft.archidata.dataAccess.options.QueryOption}:</p>
 * {@snippet lang = java :
 * final Article created = DataAccess.insert(article);
 * final Article one = DataAccess.getById(Article.class, created.getOid());
 * final List<Article> published = DataAccess.gets(Article.class,
 * 		new Condition(Filters.eq("status", "published")),
 * 		OrderBy.desc(Article::getCreatedAt),
 * 		new Limit(10));
 * }
 *
 * <p>See <code>doc/database_access.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/database_access.md">online</a>).</p>
 *
 * @see org.atriasoft.archidata.dataAccess.addOn
 * @see org.atriasoft.archidata.dataAccess.options
 * @see org.atriasoft.archidata.dataAccess.model
 * @see org.atriasoft.archidata.dataAccess.statistic
 */
package org.atriasoft.archidata.dataAccess;
