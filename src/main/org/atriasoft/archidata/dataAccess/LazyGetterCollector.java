package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.Condition;

import com.mongodb.client.model.Filters;

/**
 * Collects entity-reference loading requests across multiple rows and produces
 * batched {@link LazyGetter}s that merge individual lookups into grouped {@code $in} queries.
 *
 * <p>Instead of N individual queries (one per row), this produces one query per
 * (targetEntity, idFieldColumn) pair, eliminating the N+1 problem on RETRIEVE.
 */
public final class LazyGetterCollector {

	/** Grouping key: target entity class + the PK column used for lookup. */
	private record BatchKey(Class<?> targetEntity, String idFieldColumn) {
	}

	/** A single-value registration (ManyToOne: one ID -> one object). */
	private record SingleRegistration(Object idValue, PropertyDescriptor prop, Object targetObject) {
	}

	/** A multi-value registration (ManyToMany/OneToMany: list of IDs -> list of objects). */
	private record MultiRegistration(List<Object> idValues, PropertyDescriptor prop, Object targetObject) {
	}

	private final Map<BatchKey, List<SingleRegistration>> singleRegs = new HashMap<>();
	private final Map<BatchKey, List<MultiRegistration>> multiRegs = new HashMap<>();

	/**
	 * Register a single-value entity reference (ManyToOne).
	 * When the batch executes, the resolved entity will be set via prop.setValue(targetObject, entity).
	 */
	public void registerSingle(
			final Class<?> targetEntity,
			final String idFieldColumn,
			final Object idValue,
			final PropertyDescriptor prop,
			final Object targetObject) {
		final BatchKey key = new BatchKey(targetEntity, idFieldColumn);
		this.singleRegs.computeIfAbsent(key, k -> new ArrayList<>())
				.add(new SingleRegistration(idValue, prop, targetObject));
	}

	/**
	 * Register a multi-value entity reference (ManyToMany / OneToMany).
	 * When the batch executes, the resolved entity list will be set via prop.setValue(targetObject, list).
	 */
	public void registerMultiple(
			final Class<?> targetEntity,
			final String idFieldColumn,
			final List<Object> idValues,
			final PropertyDescriptor prop,
			final Object targetObject) {
		final BatchKey key = new BatchKey(targetEntity, idFieldColumn);
		this.multiRegs.computeIfAbsent(key, k -> new ArrayList<>())
				.add(new MultiRegistration(idValues, prop, targetObject));
	}

	/**
	 * Build batched LazyGetters from all accumulated registrations.
	 * Each (targetEntity, idFieldColumn) pair produces ONE LazyGetter that:
	 * 1. Collects all IDs (deduplicated)
	 * 2. Executes one {@code $in} query
	 * 3. Indexes results by PK
	 * 4. Distributes to each registration's target object
	 *
	 * @param ioDb The database accessor used to execute the batched queries.
	 */
	@SuppressWarnings("unchecked")
	public List<LazyGetter> buildLazyGetters(final DBAccessMongo ioDb) {
		final List<LazyGetter> result = new ArrayList<>();

		// Merge keys from both maps
		final Set<BatchKey> allKeys = new LinkedHashSet<>();
		allKeys.addAll(this.singleRegs.keySet());
		allKeys.addAll(this.multiRegs.keySet());

		for (final BatchKey key : allKeys) {
			final List<SingleRegistration> singles = this.singleRegs.getOrDefault(key, List.of());
			final List<MultiRegistration> multis = this.multiRegs.getOrDefault(key, List.of());

			// Collect all unique IDs
			final Set<Object> allIds = new LinkedHashSet<>();
			for (final SingleRegistration reg : singles) {
				if (reg.idValue() != null) {
					allIds.add(reg.idValue());
				}
			}
			for (final MultiRegistration reg : multis) {
				allIds.addAll(reg.idValues());
			}

			if (allIds.isEmpty()) {
				continue;
			}

			final Class<?> targetEntity = key.targetEntity();
			final String idFieldColumn = key.idFieldColumn();

			final LazyGetter batchGetter = (final List<LazyGetter> actionsAsync) -> {
				// One grouped query for all IDs
				final List<Object> resultList = ioDb.getsRaw(targetEntity,
						new Condition(Filters.in(idFieldColumn, allIds)));

				// Index results by PK
				final DbClassModel entityModel = DbClassModel.of(targetEntity);
				final Map<Object, Object> resultIndex = new HashMap<>();
				if (resultList != null) {
					for (final Object entity : resultList) {
						final Object pkValue = entityModel.getPrimaryKey().getProperty().getValue(entity);
						if (pkValue != null) {
							resultIndex.put(pkValue, entity);
						}
					}
				}

				// Distribute to single registrations
				for (final SingleRegistration reg : singles) {
					final Object entity = resultIndex.get(reg.idValue());
					if (entity != null) {
						reg.prop().setValue(reg.targetObject(), entity);
					}
				}

				// Distribute to multi registrations (preserve order from idValues)
				for (final MultiRegistration reg : multis) {
					final List<Object> ordered = new ArrayList<>();
					for (final Object id : reg.idValues()) {
						final Object entity = resultIndex.get(id);
						if (entity != null) {
							ordered.add(entity);
						}
					}
					if (ordered.isEmpty()) {
						reg.prop().setValue(reg.targetObject(), null);
					} else {
						reg.prop().setValue(reg.targetObject(), ordered);
					}
				}
			};

			result.add(batchGetter);
		}

		return result;
	}

	/** Check if any registrations have been accumulated. */
	public boolean isEmpty() {
		return this.singleRegs.isEmpty() && this.multiRegs.isEmpty();
	}
}
