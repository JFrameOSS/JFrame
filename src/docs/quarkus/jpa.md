# jframe-quarkus-jpa

Panache-based search repository, specification builder, page mapping, and SQL query logging for Quarkus applications with Hibernate ORM.

## SQL query logging

`DatasourceProxyProducer` wraps your `DataSource` with a logging proxy that pretty-prints all SQL queries at `DEBUG` level. Activates automatically — no configuration needed.

```
DEBUG [SLF4JQueryLoggingListener] —
    select u.id, u.name, u.email
    from users u
    where u.status = ?
```

## Search specifications

Build type-safe JPA Criteria API queries from frontend search inputs using Panache.

### Define search metadata

```java
@ApplicationScoped
public class UserSearchMetaData extends AbstractPanacheSearchMetaData {
    public UserSearchMetaData() {
        addField("name", "name", SearchType.FUZZY_TEXT, true);
        addField("email", "email", SearchType.TEXT, true);
        addField("status", "status", SearchType.ENUM, UserStatus.class, true);
        addField("createdAt", "createdAt", SearchType.DATE, true);

        // Multi-column fuzzy search
        addField("fullName",
            List.of("firstName", "lastName"),
            SearchType.MULTI_COLUMN_FUZZY, false);
    }
}
```

### Search types

Same types as Spring — see [Search types reference](../spring/jpa.md#search-types).

### Implement search repository

```java
@ApplicationScoped
public class UserRepository extends PanacheSearchRepository<User> {
    @Inject EntityManager em;

    @Override
    protected Class<User> entityClass() { return User.class; }

    @Override
    protected EntityManager entityManager() { return em; }
}
```

### Execute a search

```java
@ApplicationScoped
public class UserSearchService {
    @Inject UserRepository repository;
    @Inject UserSearchMetaData metaData;

    public PageResource<UserDto> search(SortablePageInput input) {
        PageResource<User> page = repository.search(input, metaData);
        return new UserPageMapper().map(page);
    }
}
```

### Page mapping

```java
public class UserPageMapper extends QuarkusPageMapper<User, UserDto> {
    @Override
    protected UserDto mapItem(User entity) {
        return new UserDto(entity.getId(), entity.getName());
    }
}
```

### Frontend request/response format

Same as Spring — see [Request format](../spring/jpa.md#frontend-request-format) and [Response format](../spring/jpa.md#response-format).

### Inverse search

Prefix search values with `!` to negate predicates:

```json
{ "fieldName": "status", "textValue": "!DISABLED" }
```

## Sort adapter

`PanacheSortAdapter` converts `List<SortableColumn>` to Panache `Sort`:

```java
Sort sort = PanacheSortAdapter.toSort(input.getSortOrder(), metaData);
```

This is handled automatically by `PanacheSearchRepository` — you only need it for custom queries.
