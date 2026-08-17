# jframe-spring-jpa

JPA search specifications, paginated queries, and SQL query logging for Spring Data JPA.

## SQL query logging

`DatasourceProxyConfiguration` automatically wraps every `DataSource` bean with a logging proxy. All executed SQL appears at `DEBUG` level — no configuration needed.

```
DEBUG n.t.d.l.l.SLF4JQueryLoggingListener —
    select u.id, u.name, u.email
    from users u
    where u.status = ?
```

---

## Paginated search — end-to-end

The search framework converts a single JSON request into a type-safe JPA Criteria API query with sorting, pagination, and filtering. This section walks through every layer.

### 1. Entity

Implement the `PageableItem` marker interface:

```java
@Entity
@Table(name = "\"user\"")
public class User implements PageableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<OrganizationMembership> organizations = new ArrayList<>();
}
```

### 2. Search metadata

Extend `AbstractSortSearchMetaData` to map frontend field names to entity paths and search types:

```java
@Component
public class UserSearchMetaData extends AbstractSortSearchMetaData {

    public UserSearchMetaData() {
        // addField(frontendName, entityPath, searchType, sortable)
        addField("email",     "email",     SearchType.TEXT,       true);
        addField("enabled",   "enabled",   SearchType.BOOLEAN,    true);
        addField("role",      "role",      SearchType.MULTI_ENUM, Role.class, true);
        addField("createdAt", "createdAt", SearchType.DATE,       true);

        // Numeric field — supports JPA path traversal for joins
        addField("organization", "organizations.organization.id",
            SearchType.NUMERIC, true);

        // Multi-column fuzzy — one search term across multiple columns
        addField("search",
            List.of("email", "firstName", "lastName"),
            SearchType.MULTI_COLUMN_FUZZY, true);
    }
}
```

> **Entity paths** follow JPA Criteria API conventions. `organizations.organization.id` traverses `User.organizations → OrganizationMembership.organization → Organization.id`.

### 3. Repository

Your Spring Data JPA repository must extend **both** `JpaRepository` and `JpaSpecificationExecutor`:

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>,
                                        JpaSpecificationExecutor<User> {

    // Override findAll to eagerly fetch associations and avoid N+1 queries
    @Override
    @EntityGraph(attributePaths = "organizations.organization")
    Page<User> findAll(Specification<User> spec, Pageable pageable);
}
```

### 4. Service

Use the convenience methods on `AbstractSortSearchMetaData` to build the pageable and specification in one step:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSearchMetaData userMetaData;

    public Page<User> searchUsers(SortablePageInput input) {
        Pageable pageable = userMetaData.toPageable(input);
        JpaSearchSpecification<User> spec = userMetaData.toSearchSpecification(input);
        return userRepository.findAll(spec, pageable);
    }
}
```

**Alternative:** Extend `PagedSearchingService` to skip the manual wiring:

```java
@Service
@RequiredArgsConstructor
public class UserService extends PagedSearchingService {

    private final UserRepository userRepository;
    private final UserSearchMetaData userMetaData;

    public Page<User> searchUsers(SortablePageInput input) {
        return searchPage(input, userMetaData, userRepository);
    }
}
```

**Scoped queries** — restrict results to a tenant, organization, or parent entity:

```java
public Page<User> searchUsersInOrganization(SortablePageInput input, Long orgId) {
    Pageable pageable = userMetaData.toPageable(input);
    Specification<User> spec =
        userMetaData.toSearchSpecification(input, "organizations.organization.id", orgId);
    return userRepository.findAll(spec, pageable);
}
```

`toSearchSpecification(input, fieldPath, scopeValue)` ANDs an equality predicate on `fieldPath` with the base search spec. Dot-separated paths (e.g. `"tenant.id"`) are traversed automatically.

### 5. Response DTO

Implement `PageableItemResource`:

```java
@Data
@NoArgsConstructor
public class UserDetailsResponse implements PageableItemResource {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Role role;
    private ZonedDateTime createdAt;
}
```

### 6. Page mapper

Extend `PageMapper<DTO, Entity>` and use MapStruct for the entity→DTO conversion:

```java
@Mapper(config = SharedMapperConfig.class, uses = DateTimeMapper.class)
public abstract class UserDetailsMapper extends PageMapper<UserDetailsResponse, User> {

    @Override
    public abstract UserDetailsResponse toResourceObject(User user);
}
```

`PageMapper.toPageResource(Page<User>)` iterates the page content, calls `toResourceObject` for each entity, and returns `PageResource<UserDetailsResponse>`.

Or use `SpringPageAdapter` directly without a mapper class:

```java
PageResource<UserDto> result = SpringPageAdapter.toPageResource(
    page.map(user -> new UserDto(user.getId(), user.getName()))
);
```

### 7. Controller

```java
@RestController
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final UserDetailsMapper userDetailsMapper;

    @PostMapping(path = "/api/admin/users/search", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<PageResource<UserDetailsResponse>> searchUsers(
            @RequestBody SortablePageInput input) {
        Page<User> page = userService.searchUsers(input);
        return ResponseEntity.ok(userDetailsMapper.toPageResource(page));
    }
}
```

---

## Convenience methods on `AbstractSortSearchMetaData`

| Method | Returns | Description |
|--------|---------|-------------|
| `toPageable(SortablePageInput)` | `Pageable` | Builds a `PageRequest` from the input. Falls back to `getDefaultPageSize()` when page size ≤ 0. |
| `toSearchSpecification(SortablePageInput)` | `JpaSearchSpecification<T>` | Builds a specification from the input's search criteria. |
| `toSearchSpecification(SortablePageInput, String, Object)` | `Specification<T>` | Same as above, ANDed with an equality predicate on the given field path. Supports nested paths (`"tenant.id"`). |
| `getDefaultPageSize()` | `int` | Returns `20`. Override in subclass to change the default. |
| `toSort(List<SortableColumn>)` | `Sort` | Returns `Sort.unsorted()` for null/empty input. Throws `IllegalArgumentException` for non-sortable fields. |
| `toSearchCriteria(List<SearchInput>)` | `List<SearchCriterium>` | Converts search inputs to criteria. Returns empty list for null/empty input. |

### Overriding the default page size

```java
@Component
public class UserSearchMetaData extends AbstractSortSearchMetaData {

    @Override
    protected int getDefaultPageSize() {
        return 50;  // instead of 20
    }

    // ... addField() calls
}
```

---

## Frontend request format

The client sends a `SortablePageInput` as JSON:

```json
{
  "pageNumber": 0,
  "pageSize": 20,
  "sortOrder": [
    { "name": "createdAt", "direction": "DESC" }
  ],
  "searchInputs": [
    { "fieldName": "search", "textValue": "john" },
    { "fieldName": "role", "textValueList": ["ADMIN", "MODERATOR"] },
    { "fieldName": "enabled", "textValue": "true" },
    { "fieldName": "createdAt", "fromDateValue": "2025-01-01T00:00:00Z", "toDateValue": "2025-12-31T23:59:59Z" }
  ]
}
```

### SearchInput fields per SearchType

| SearchType | Use `textValue` | Use `textValueList` | Use `fromDateValue` + `toDateValue` | Use `fromNumericValue` + `toNumericValue` | `operator` |
|-----------|:-:|:-:|:-:|:-:|:-:|
| `TEXT` | ✅ | — | — | — | — |
| `FUZZY_TEXT` | ✅ | — | — | — | — |
| `NUMERIC` | ✅ | — | — | — | — |
| `BOOLEAN` | ✅ | — | — | — | — |
| `ENUM` | ✅ | — | — | — | — |
| `MULTI_TEXT` | — | ✅ | — | — | — |
| `MULTI_ENUM` | — | ✅ | — | — | — |
| `MULTI_FUZZY` | — | ✅ | — | — | `AND` / `OR` |
| `MULTI_COLUMN_FUZZY` | ✅ | — | — | — | — |
| `DATE` | — | — | ✅ | — | — |
| `MULTI_NUMERIC` | — | ✅ (parsed as integers) | — | — | — |
| `NUMERIC_RANGE` | — | — | — | ✅ (both nullable) | — |

### Inverse search

Prefix any `textValue` with `!` to negate the predicate:

```json
{ "fieldName": "role", "textValue": "!DISABLED" }
```

Generates `role != 'DISABLED'` instead of `role = 'DISABLED'`.

## Response format

```json
{
  "totalElements": 142,
  "totalPages": 8,
  "pageSize": 20,
  "pageNumber": 0,
  "content": [
    { "id": 1, "email": "john@example.com", "firstName": "John", ... },
    ...
  ]
}
```

---

## Search types reference

| SearchType | SQL equivalent | Typical use |
|-----------|---------------|-------------|
| `TEXT` | `= ?` | Exact match (email, username) |
| `FUZZY_TEXT` | `LOWER(col) LIKE LOWER(%?%)` | Case-insensitive contains |
| `MULTI_TEXT` | `IN (?, ?, ...)` | Multiple exact values |
| `MULTI_FUZZY` | `LIKE %?% AND/OR LIKE %?%` | Multiple fuzzy terms |
| `MULTI_COLUMN_FUZZY` | `col1 LIKE %?% OR col2 LIKE %?%` | Global search box |
| `NUMERIC` | `= ?` | Numeric equality |
| `MULTI_NUMERIC` | `IN (?, ?, ...)` | Multiple numeric values |
| `NUMERIC_RANGE` | `>= ? AND/OR <= ?` | Numeric range (from/to, both nullable) |
| `BOOLEAN` | `= ?` | Boolean flag |
| `DATE` | `>= ? AND <= ?` | Date range (from/to) |
| `ENUM` | `= ?` | Single enum value |
| `MULTI_ENUM` | `IN (?, ?, ...)` | Multiple enum values |
