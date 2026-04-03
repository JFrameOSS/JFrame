# jframe-quarkus-jpa — AI Context

> Quarkus Hibernate/Panache JPA adapter for jframe. Search specification, pagination, sorting, and SQL query logging.

## Search Specification

`PanacheSearchSpecification<T>` extends `BaseSearchSpecification<T>` — wraps search criteria into JPA Criteria API predicates.

```java
List<SearchCriterium> criteria = metaData.toSearchCriteria(searchInputs);
var spec = new PanacheSearchSpecification<User>(criteria);
PageResource<User> page = repository.searchPage(spec, 0, 20, sort);
```

## Search Metadata

`AbstractPanacheSearchMetaData` — maps frontend field names to DB columns with `SearchType`.

```java
@ApplicationScoped
public class UserSearchMetaData extends AbstractPanacheSearchMetaData {
    public UserSearchMetaData() {
        addField("name", "name", SearchType.FUZZY_TEXT, true);
        addField("email", "email", SearchType.TEXT, true);
        addField("status", "status", SearchType.ENUM, UserStatus.class, true);
        addField("createdDate", "createdAt", SearchType.DATE, true);
        addField("quickSearch", List.of("name", "email"),
                 SearchType.MULTI_COLUMN_FUZZY, false);
    }
}
```

**Methods:**
- `toSearchCriteria(List<SearchInput>)` → `List<SearchCriterium>`
- `toSort(List<SortableColumn>)` → Panache `Sort` (returns null if empty)

## Repository

`PanacheSearchRepository<T extends PageableItem>` — abstract base for specification-based paginated queries.

```java
@ApplicationScoped
public class UserRepository extends PanacheSearchRepository<User> {
    @Inject EntityManager em;

    @Override protected Class<User> entityClass() { return User.class; }
    @Override protected EntityManager entityManager() { return em; }
}
```

**Key method:**
```java
PageResource<T> searchPage(SearchSpecification<T> spec, int pageNumber, int pageSize, Sort sort)
```
- Builds JPA Criteria query with spec as WHERE predicate
- Applies Panache Sort as ORDER BY
- Executes data + count queries
- Returns `PageResource<T>`

## Pagination & Mapping

- `QuarkusPageAdapter.toPageResource(content, totalElements, totalPages, pageSize, pageNumber)` — converts raw results to `PageResource<T>`
- `QuarkusPageMapper<S, T>` — abstract entity→DTO page mapper

```java
@ApplicationScoped
public class UserPageMapper extends QuarkusPageMapper<User, UserDto> {
    @Override protected UserDto mapItem(User user) {
        return new UserDto(user.getId(), user.getName());
    }
}
```

## Sorting

`PanacheSortAdapter.toSort(List<SortableColumn>)` — converts `SortableColumn` list to Panache `Sort`. Returns null if input is null, `Sort.empty()` if empty.

## SQL Query Logging

`DatasourceProxyProducer` — CDI producer (`@Alternative @Priority(1)`) wraps Agroal `DataSource` with `ProxyDataSource`.

Enable via:
```properties
quarkus.arc.selected-alternatives=io.github.jframe.datasource.config.DatasourceProxyProducer
```

Uses `PrettyQueryEntryCreator` (Hibernate FormatStyle) + `DefaultQueryExecutionListener`.

## Entity Requirements

- Must implement `PageableItem` (marker interface)
- Standard JPA `@Entity` — no base entity classes provided
- Auditing: use JPA lifecycle callbacks (`@PrePersist`, `@PreUpdate`) or custom listeners
