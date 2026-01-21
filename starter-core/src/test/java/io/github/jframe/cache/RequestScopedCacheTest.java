package io.github.jframe.cache;

import io.github.support.UnitTest;
import io.github.support.fixtures.TestEntity;
import io.github.support.fixtures.TestEntityCache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link RequestScopedCache}.
 *
 * <p>Verifies the functionality of the request-scoped cache including:
 * <ul>
 * <li>Basic put and get operations</li>
 * <li>Bulk putAll and getAll operations</li>
 * <li>getOrLoad and getAllOrLoad with loader functions</li>
 * <li>Handling of nulls and edge cases</li>
 * <li>Remove and clear operations</li>
 * </ul>
 */
@DisplayName("Unit Test - Request Scoped Cache")
class RequestScopedCacheTest extends UnitTest {

    private TestEntityCache cache;

    @Override
    @BeforeEach
    public void setUp() {
        cache = new TestEntityCache();
    }

    @Nested
    @DisplayName("put and get operations")
    class PutAndGetOperations {

        @Test
        @DisplayName("Should store and retrieve entity")
        void shouldStoreAndRetrieveEntity() {
            // Given: An entity with ID 1
            final TestEntity entity = aTestEntity();

            // When: Storing and retrieving the entity
            cache.put(entity);
            final Optional<TestEntity> result = cache.get(1L);

            // Then: Entity should be retrieved successfully
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().name(), is("Test"));
        }

        @Test
        @DisplayName("Should return empty for non-existent entity")
        void shouldReturnEmptyForNonExistent() {
            // Given: Cache with no entities

            // When: Requesting non-existent entity with ID 999
            final Optional<TestEntity> result = cache.get(999L);

            // Then: Result should be empty
            assertThat(result.isEmpty(), is(true));
        }

        @Test
        @DisplayName("Should ignore null entity")
        void shouldIgnoreNullEntity() {
            // Given: A null entity

            // When: Attempting to store null
            cache.put(null);

            // Then: Cache should remain empty
            assertThat(cache.size(), is(0));
        }

        @Test
        @DisplayName("Should ignore entity with null ID")
        void shouldIgnoreEntityWithNullId() {
            // Given: An entity with null ID
            final TestEntity entity = aTestEntityWithNullId();

            // When: Attempting to store entity with null ID
            cache.put(entity);

            // Then: Cache should remain empty
            assertThat(cache.size(), is(0));
        }
    }


    @Nested
    @DisplayName("putAll operations")
    class PutAllOperations {

        @Test
        @DisplayName("Should store multiple entities")
        void shouldStoreMultipleEntities() {
            // Given: Multiple test entities
            final List<TestEntity> entities = aTestEntityList();

            // When: Storing all entities
            cache.putAll(entities);

            // Then: All entities should be stored
            assertThat(cache.size(), is(3));
            assertThat(cache.get(1L).get().name(), is("First"));
            assertThat(cache.get(2L).get().name(), is("Second"));
            assertThat(cache.get(3L).get().name(), is("Third"));
        }

        @Test
        @DisplayName("Should ignore null collection")
        void shouldIgnoreNullCollection() {
            // Given: A null collection

            // When: Attempting to store null collection
            cache.putAll(null);

            // Then: Cache should remain empty
            assertThat(cache.size(), is(0));
        }
    }


    @Nested
    @DisplayName("getOrLoad operations")
    class GetOrLoadOperations {

        @Test
        @DisplayName("Should return cached entity without loading")
        void shouldReturnCachedWithoutLoading() {
            // Given: A cached entity with ID 1
            final TestEntity entity = new TestEntity(1L, "Cached");
            cache.put(entity);

            // When: Requesting entity with getOrLoad
            final Optional<TestEntity> result = cache.getOrLoad(
                1L,
                id -> {
                    throw new AssertionError("Loader should not be called");
                }
            );

            // Then: Cached entity should be returned without invoking loader
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().name(), is("Cached"));
        }

        @Test
        @DisplayName("Should load and cache entity when not present")
        void shouldLoadAndCacheWhenNotPresent() {
            // Given: An entity that will be loaded
            final TestEntity loaded = new TestEntity(1L, "Loaded");

            // When: Requesting entity not in cache
            final Optional<TestEntity> result = cache.getOrLoad(1L, id -> Optional.of(loaded));

            // Then: Entity should be loaded and cached
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().name(), is("Loaded"));
            assertThat(cache.contains(1L), is(true));
        }

        @Test
        @DisplayName("Should return empty when loader returns empty")
        void shouldReturnEmptyWhenLoaderReturnsEmpty() {
            // Given: A loader that returns empty

            // When: Requesting entity that doesn't exist
            final Optional<TestEntity> result = cache.getOrLoad(1L, id -> Optional.empty());

            // Then: Result should be empty and not cached
            assertThat(result.isEmpty(), is(true));
            assertThat(cache.contains(1L), is(false));
        }
    }


    @Nested
    @DisplayName("getAll operations")
    class GetAllOperations {

        @Test
        @DisplayName("Should return all cached entities")
        void shouldReturnAllCachedEntities() {
            // Given: Two cached entities
            cache.put(new TestEntity(1L, "First"));
            cache.put(new TestEntity(2L, "Second"));

            // When: Requesting all cached entities
            final Map<Long, TestEntity> result = cache.getAll();

            // Then: All entities should be returned
            assertThat(result.size(), is(2));
            assertThat(result.keySet(), containsInAnyOrder(1L, 2L));
        }

        @Test
        @DisplayName("Should return only requested IDs that are cached")
        void shouldReturnOnlyRequestedCachedIds() {
            // Given: Three cached entities
            cache.put(new TestEntity(1L, "First"));
            cache.put(new TestEntity(2L, "Second"));
            cache.put(new TestEntity(3L, "Third"));

            // When: Requesting specific IDs including non-existent one
            final Map<Long, TestEntity> result = cache.getAll(Set.of(1L, 3L, 999L));

            // Then: Only requested cached entities should be returned
            assertThat(result.size(), is(2));
            assertThat(result.keySet(), containsInAnyOrder(1L, 3L));
        }

        @Test
        @DisplayName("Should return empty map for null IDs")
        void shouldReturnEmptyForNullIds() {
            // Given: A cached entity
            cache.put(new TestEntity(1L, "First"));

            // When: Requesting with null IDs
            final Map<Long, TestEntity> result = cache.getAll(null);

            // Then: Result should be empty
            assertThat(result.isEmpty(), is(true));
        }
    }


    @Nested
    @DisplayName("getAllOrLoad operations")
    class GetAllOrLoadOperations {

        @Test
        @DisplayName("Should return cached entities without loading")
        void shouldReturnCachedWithoutLoading() {
            // Given: Two cached entities
            cache.put(new TestEntity(1L, "Cached1"));
            cache.put(new TestEntity(2L, "Cached2"));

            // When: Requesting all cached entities
            final Map<Long, TestEntity> result = cache.getAllOrLoad(
                Set.of(1L, 2L),
                ids -> {
                    throw new AssertionError("Loader should not be called when all entities are cached");
                }
            );

            // Then: Cached entities should be returned without invoking loader
            assertThat(result.size(), is(2));
            assertThat(result.get(1L).name(), is("Cached1"));
            assertThat(result.get(2L).name(), is("Cached2"));
        }

        @Test
        @DisplayName("Should load missing entities and cache them")
        void shouldLoadMissingAndCache() {
            // Given: One cached entity
            cache.put(new TestEntity(1L, "Cached"));

            // When: Requesting three entities (one cached, two missing)
            final Map<Long, TestEntity> result = cache.getAllOrLoad(
                Set.of(1L, 2L, 3L),
                ids -> List.of(new TestEntity(2L, "Loaded2"), new TestEntity(3L, "Loaded3"))
            );

            // Then: All entities should be returned
            assertThat(result.size(), is(3));
            assertThat(result.get(1L).name(), is("Cached"));
            assertThat(result.get(2L).name(), is("Loaded2"));
            assertThat(result.get(3L).name(), is("Loaded3"));

            // And: Loaded entities are now cached
            assertThat(cache.contains(2L), is(true));
            assertThat(cache.contains(3L), is(true));
        }

        @Test
        @DisplayName("Should only load missing IDs")
        void shouldOnlyLoadMissingIds() {
            // Given: One cached entity
            cache.put(new TestEntity(1L, "Cached"));

            // When: Requesting two entities (one cached, one missing)
            final Map<Long, TestEntity> result = cache.getAllOrLoad(
                Set.of(1L, 2L),
                ids -> {
                    // Verify only missing ID is requested
                    assertThat(ids, containsInAnyOrder(2L));
                    return List.of(new TestEntity(2L, "Loaded"));
                }
            );

            // Then: Both entities should be returned
            assertThat(result.size(), is(2));
        }

        @Test
        @DisplayName("Should return empty map for null IDs")
        void shouldReturnEmptyForNullIds() {
            // Given: A null ID set

            // When: Requesting with null IDs
            final Map<Long, TestEntity> result = cache.getAllOrLoad(
                null,
                ids -> {
                    throw new AssertionError("Loader should not be called for null IDs");
                }
            );

            // Then: Result should be empty
            assertThat(result.isEmpty(), is(true));
        }

        @Test
        @DisplayName("Should return empty map for empty IDs")
        void shouldReturnEmptyForEmptyIds() {
            // Given: An empty ID set

            // When: Requesting with empty IDs
            final Map<Long, TestEntity> result = cache.getAllOrLoad(
                Set.of(),
                ids -> {
                    throw new AssertionError("Loader should not be called for empty IDs");
                }
            );

            // Then: Result should be empty
            assertThat(result.isEmpty(), is(true));
        }

        @Test
        @DisplayName("Should handle loader returning partial results")
        void shouldHandlePartialLoaderResults() {
            // Given: Requesting 3 IDs, but loader only returns 2

            // When: Requesting entities with partial loader results
            final Map<Long, TestEntity> result = cache.getAllOrLoad(
                Set.of(1L, 2L, 3L),
                ids -> List.of(new TestEntity(1L, "Found1"), new TestEntity(2L, "Found2"))
                // Note: ID 3 is not returned by loader
            );

            // Then: Only found entities are in result
            assertThat(result.size(), is(2));
            assertThat(result.containsKey(3L), is(false));
        }
    }


    @Nested
    @DisplayName("remove and clear operations")
    class RemoveAndClearOperations {

        @Test
        @DisplayName("Should remove entity from cache")
        void shouldRemoveEntity() {
            // Given: A cached entity
            cache.put(aTestEntity());

            // When: Removing the entity
            final TestEntity removed = cache.remove(1L);

            // Then: Entity should be removed and returned
            assertThat(removed.name(), is("Test"));
            assertThat(cache.contains(1L), is(false));
        }

        @Test
        @DisplayName("Should clear all entities")
        void shouldClearAllEntities() {
            // Given: Multiple cached entities
            cache.put(new TestEntity(1L, "First"));
            cache.put(new TestEntity(2L, "Second"));

            // When: Clearing the cache
            cache.clear();

            // Then: Cache should be empty
            assertThat(cache.size(), is(0));
        }
    }
}
