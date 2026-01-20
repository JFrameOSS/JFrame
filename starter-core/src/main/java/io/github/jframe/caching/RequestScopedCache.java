package io.github.jframe.caching;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic request-scoped cache for entities.
 *
 * <p>Eliminates duplicate database queries within a single HTTP request.
 * The cache is automatically discarded when the request scope ends.
 *
 * <p>Usage: Extend this class with {@code @Component} and {@code @RequestScope}
 * and specify the entity type and identifier type.
 *
 * <pre>
 * {@code
 * @Component
 * @RequestScope
 * public class ChannelCache extends RequestScopedCache<Long, Entity> {
 *     @Override
 *     protected Long getId(Entity entity) {
 *         return entity.getId();
 *     }
 * }
 * }
 * </pre>
 *
 * @param <K> the type of the entity identifier (e.g. {@link Long}, {@link UUID})
 * @param <V> the type of the cached entity (e.g. Channel, User)
 */
public abstract class RequestScopedCache<K, V> {

    private final Map<K, V> cache = new ConcurrentHashMap<>();

    /**
     * Extracts the identifier from an entity.
     *
     * @param entity the entity instance
     * @return the unique identifier of the entity, or {@code null} if it cannot be determined
     */
    protected abstract K getId(V entity);

    /**
     * Stores a single entity in the cache.
     *
     * @param entity the entity to cache (ignored if {@code null} or its id is {@code null})
     */
    public void put(final V entity) {
        if (entity == null) {
            return;
        }

        final K id = getId(entity);
        if (id != null) {
            cache.put(id, entity);
        }
    }

    /**
     * Stores multiple entities in the cache.
     *
     * @param entities collection of entities to cache (ignored if {@code null} or empty)
     */
    public void putAll(final Collection<V> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach(this::put);
    }

    /**
     * Retrieves an entity from the cache.
     *
     * @param id the entity identifier
     * @return an {@link Optional} containing the cached entity, or empty if not present
     */
    public Optional<V> get(final K id) {
        return Optional.ofNullable(cache.get(id));
    }

    /**
     * Retrieves an entity from the cache or loads it using the given loader function.
     * <p>
     * The loading operation is atomic: the loader will only be invoked if the value is not already present in the cache.
     *
     * @param id     the entity identifier
     * @param loader function used to load the entity if absent
     * @return an {@link Optional} containing the cached or loaded entity
     */
    public Optional<V> getOrLoad(final K id, final Function<K, Optional<V>> loader) {
        Objects.requireNonNull(loader, "loader");

        return Optional.ofNullable(
            cache.computeIfAbsent(id, key -> loader.apply(key).orElse(null))
        );
    }

    /**
     * Returns an unmodifiable view of all cached entries.
     *
     * @return unmodifiable map of identifiers to entities
     */
    public Map<K, V> getAll() {
        return Collections.unmodifiableMap(cache);
    }

    /**
     * Retrieves all cached entities for the given identifiers.
     *
     * @param ids collection of entity identifiers
     * @return unmodifiable map of identifier to entity for all cached entries
     */
    public Map<K, V> getAll(final Collection<K> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return ids.stream()
            .map(id -> Map.entry(id, cache.get(id)))
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Retrieves all entities for the given identifiers, loading missing ones in a single batch.
     * <p>
     * Newly loaded entities are automatically cached.
     *
     * @param ids         collection of entity identifiers
     * @param batchLoader function that loads entities for the missing identifiers
     * @return map of identifier to entity for all found entities
     */
    public Map<K, V> getAllOrLoad(
        final Collection<K> ids,
        final Function<Collection<K>, Collection<V>> batchLoader
    ) {
        Objects.requireNonNull(batchLoader, "batchLoader");

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<K, V> result = new HashMap<>(ids.size());
        final List<K> missing = new ArrayList<>();

        for (K id : ids) {
            V cached = cache.get(id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) {
            final Collection<V> loaded = batchLoader.apply(missing);
            if (loaded != null && !loaded.isEmpty()) {
                for (V entity : loaded) {
                    if (entity == null) {
                        continue;
                    }

                    K id = getId(entity);
                    if (id != null) {
                        cache.put(id, entity);
                        result.put(id, entity);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks whether an entity with the given identifier is cached.
     *
     * @param id the entity identifier
     * @return {@code true} if present in the cache, {@code false} otherwise
     */
    public boolean contains(final K id) {
        return cache.containsKey(id);
    }

    /**
     * Returns the number of cached entities.
     *
     * @return cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Removes all entries from the cache.
     * <p>
     * Normally not required, as request-scoped beans are destroyed automatically.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Removes a single entity from the cache.
     *
     * @param id the entity identifier
     * @return the removed entity, or {@code null} if not present
     */
    public V remove(final K id) {
        return cache.remove(id);
    }
}
