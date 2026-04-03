# jframe-spring-jpa — AI Context

> Spring Data JPA adapter for jframe. Provides search specification integration, pagination, and SQL query logging.

## Search Specification

`JpaSearchSpecification<T>` extends `BaseSearchSpecification<T>` and implements Spring's `Specification<T>`.

```java
List<SearchCriterium> criteria = metaData.toSearchCriteria(searchInputs);
var spec = new JpaSearchSpecification<User>(criteria);
Page<User> page = repository.findAll(spec, pageable);
```

Composable with `spec.and(otherSpec)` and `spec.or(otherSpec)`.

## Search Metadata

`AbstractSortSearchMetaData` — maps frontend field names to DB columns with `SearchType`.

```java
@Component
public class UserSearchMetaData extends AbstractSortSearchMetaData {
    public UserSearchMetaData() {
        addField("name", "user_name", SearchType.FUZZY_TEXT, true);
        addField("status", "status", SearchType.ENUM, UserStatus.class, true);
        addField("createdDate", "created_at", SearchType.DATE, true);
        addField("quickSearch", List.of("first_name", "last_name"),
                 SearchType.MULTI_COLUMN_FUZZY, false);
    }
}
```

**Methods:**
- `toSearchCriteria(List<SearchInput>)` → `List<SearchCriterium>`
- `toSort(List<SortableColumn>)` → Spring `Sort`

## Paginated Search Service

`PagedSearchingService` — abstract base for repository search.

```java
@Service
public class UserSearchService extends PagedSearchingService {
    public Page<User> search(SortablePageInput input) {
        return searchPage(input, metadata, repository);
    }
}
```

**Overloads:**
- `searchPage(input, metaData, repository)` — auto-builds spec from input
- `searchPage(input, metaData, spec, repository)` — uses pre-built spec

## Pagination Adapters

- `SpringPageAdapter.toPageResource(Page<T>)` → `PageResource<T>` — converts Spring Page to framework DTO
- `PageMapper<T extends PageableItemResource, S extends PageableItem>` — abstract entity→DTO page mapper

```java
@Component
public class UserMapper extends PageMapper<UserResource, User> {
    @Override
    public UserResource toResourceObject(User source) {
        return new UserResource(source.getId(), source.getName());
    }
}
```

## SQL Query Logging

`DatasourceProxyConfiguration` — `BeanPostProcessor` that auto-wraps all `DataSource` beans with `ProxyDataSource`.
- Uses `PrettyQueryEntryCreator` (Hibernate FormatStyle: NONE, BASIC, DDL, HIGHLIGHT)
- Logs via `DefaultQueryExecutionListener` (from jframe-core)
- No manual configuration required

## Repository Pattern

No base repository provided. Use standard Spring Data:

```java
public interface UserRepository extends
    JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {
}
```

Entities must implement `PageableItem` (marker interface from jframe-core).

## No Auto-Configuration

This module has no `@AutoConfiguration`. Add manually:

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.mycompany.repository")
@EntityScan(basePackages = "com.mycompany.entity")
public class JpaConfig {}
```
