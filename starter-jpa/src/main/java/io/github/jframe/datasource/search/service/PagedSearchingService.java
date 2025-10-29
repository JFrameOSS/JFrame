package io.github.jframe.datasource.search.service;

import io.github.jframe.datasource.search.model.AbstractSortSearchMetaData;
import io.github.jframe.datasource.search.model.JpaSearchSpecification;
import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.input.SortablePageInput;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Abstract base class for paginated searching of JPA repositories.
 *
 * <p>Provides standardized search functionality by converting user input into JPA specifications
 * and pagination parameters. Concrete implementations should extend this class for domain-specific search services.
 *
 * @see AbstractSortSearchMetaData
 * @see SortablePageInput
 * @see PageableItem
 */
public abstract class PagedSearchingService {

    /**
     * Retrieves a paginated collection of entities based on search criteria.
     *
     * <p>Converts the search inputs to JPA criteria using the provided metadata and delegates
     * to the specification-based search method.
     *
     * @param <T>        the entity type extending {@link PageableItem}
     * @param input      pagination, sorting, and search parameters. Must not be {@code null}.
     * @param metaData   domain-specific metadata for search criteria and sort configuration. Must not be {@code null}.
     * @param repository JPA repository for query execution. Must not be {@code null}.
     * @return page containing matching entities and pagination metadata. Never {@code null}.
     * @throws IllegalArgumentException                    if any parameter is {@code null}
     * @throws org.springframework.dao.DataAccessException if database access fails
     */
    protected <T extends PageableItem> Page<T> searchPage(final SortablePageInput input,
        final AbstractSortSearchMetaData metaData,
        final JpaSpecificationExecutor<T> repository) {
        final JpaSearchSpecification<T> searchSpecification =
            new JpaSearchSpecification<>(metaData.toSearchCriteria(input.getSearchInputs()));
        return searchPage(input, metaData, searchSpecification, repository);
    }

    /**
     * Retrieves a paginated collection of entities using a pre-constructed JPA specification.
     *
     * @param <T>                 the entity type extending {@link PageableItem}
     * @param input               pagination and sorting parameters. Must not be {@code null}.
     * @param metaData            metadata for sort configuration. Must not be {@code null}.
     * @param searchSpecification JPA specification defining the query criteria. Must not be {@code null}.
     * @param repository          JPA repository for query execution. Must not be {@code null}.
     * @return page containing matching entities and pagination metadata. Never {@code null}.
     * @throws IllegalArgumentException                    if any parameter is {@code null} or pagination parameters are invalid
     * @throws org.springframework.dao.DataAccessException if database access fails
     */
    protected <T extends PageableItem> Page<T> searchPage(final SortablePageInput input,
        final AbstractSortSearchMetaData metaData,
        final Specification<T> searchSpecification,
        final JpaSpecificationExecutor<T> repository) {
        final Pageable page = PageRequest.of(input.getPageNumber(), input.getPageSize(), metaData.toSort(input.getSortOrder()));
        return repository.findAll(searchSpecification, page);
    }
}
