package io.github.support.fixtures;


import io.github.jframe.cache.RequestScopedCache;

/**
 * Request-scoped cache for TestEntity entities.
 */
public class TestEntityCache extends RequestScopedCache<Long, TestEntity> {

    @Override
    protected Long getId(final TestEntity entity) {
        return entity.id();
    }
}
