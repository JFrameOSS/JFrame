# jframe-spring-jpa

JPA search specifications, paginated queries, and SQL query logging for Spring Data JPA applications.

## SQL query logging

Automatically wraps your `DataSource` with a logging proxy that pretty-prints all SQL queries at `DEBUG` level. No configuration needed — just add the dependency.

```
DEBUG n.t.d.l.l.SLF4JQueryLoggingListener —
    select u.id, u.name, u.email
    from users u
    where u.status = ?
```

## Search specifications

Build type-safe JPA Criteria API queries from frontend search inputs.

### Define search metadata

Map frontend field names to database columns:

```java
@Component
public class UserSearchMetaData extends AbstractSortSearchMetaData {
    public UserSearchMetaData() {
        // addField(frontendName, dbColumn, searchType, sortable)
        addField("name", "user.name", SearchType.FUZZY_TEXT, true);
        addField("email", "user.email", SearchType.TEXT, true);
        addField("status", "user.status", SearchType.ENUM, UserStatus.class, true);
        addField("role", "user.role", SearchType.MULTI_ENUM, Role.class, false);
        addField("createdAt", "user.createdAt", SearchType.DATE, true);
        addField("age", "user.age", SearchType.NUMERIC, true);
        addField("active", "user.active", SearchType.BOOLEAN, false);

        // Multi-column fuzzy: search across multiple columns with one term
        addField("fullName",
            List.of("user.firstName", "user.lastName"),
            SearchType.MULTI_COLUMN_FUZZY, false);
    }
}
```

### Search types

| SearchType | SQL operator | Example input |
|-----------|-------------|---------------|
| `TEXT` | `= ?` | `"john@example.com"` |
| `FUZZY_TEXT` | `LIKE %?%` | `"john"` |
| `MULTI_TEXT` | `IN (?, ?, ...)` | `["admin", "user"]` |
| `MULTI_FUZZY` | `LIKE %?% AND/OR LIKE %?%` | `["john", "doe"]` |
| `MULTI_COLUMN_FUZZY` | `col1 LIKE %?% OR col2 LIKE %?%` | `"john"` |
| `NUMERIC` | `= ?` | `42` |
| `BOOLEAN` | `= ?` | `true` |
| `DATE` | `BETWEEN ? AND ?` | from/to dates |
| `ENUM` | `= ?` | `"ACTIVE"` |
| `MULTI_ENUM` | `IN (?, ?, ...)` | `["ACTIVE", "PENDING"]` |

### Inverse search

Prefix any search value with `!` to negate the predicate:

```json
{ "fieldName": "status", "textValue": "!DISABLED" }
```

This generates `status != 'DISABLED'` instead of `status = 'DISABLED'`.

### Build and execute a search

```java
@Service
@RequiredArgsConstructor
public class UserSearchService extends PagedSearchingService {
    private final UserRepository userRepository;
    private final UserSearchMetaData metaData;

    public PageResource<UserDto> search(SortablePageInput input) {
        Page<User> page = searchPage(input, metaData, userRepository);
        return SpringPageAdapter.toPageResource(page.map(this::toDto));
    }
}
```

### Frontend request format

```json
{
  "pageNumber": 0,
  "pageSize": 20,
  "sortOrder": [
    { "column": "name", "direction": "ASC" }
  ],
  "searchInputs": [
    { "fieldName": "name", "textValue": "john" },
    { "fieldName": "status", "textValue": "ACTIVE" }
  ]
}
```

### Response format

```json
{
  "totalElements": 142,
  "totalPages": 8,
  "pageSize": 20,
  "pageNumber": 0,
  "content": [ ... ]
}
```

## Page mapping

Convert `Page<Entity>` to `PageResource<DTO>`:

```java
public class UserPageMapper extends PageMapper<UserDto, User> {
    @Override
    protected UserDto toResourceObject(User entity) {
        return new UserDto(entity.getId(), entity.getName());
    }
}
```

Or use `SpringPageAdapter` directly:

```java
PageResource<UserDto> resource = SpringPageAdapter.toPageResource(
    page.map(userMapper::toDto)
);
```
