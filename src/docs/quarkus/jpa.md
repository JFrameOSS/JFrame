# jframe-quarkus-jpa

Panache-based search repository, specification builder, page mapping, and SQL query logging for Quarkus with Hibernate ORM.

## SQL query logging

`DatasourceProxyProducer` wraps your Agroal `DataSource` with a logging proxy. All SQL appears at `DEBUG` level — no configuration needed.

```
DEBUG [SLF4JQueryLoggingListener] —
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

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<OrganizationMembership> organizations = new ArrayList<>();
}
```

### 2. Search metadata

Extend `AbstractPanacheSearchMetaData` to map frontend field names to entity paths and search types:

```java
@ApplicationScoped
public class UserSearchMetaData extends AbstractPanacheSearchMetaData {

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

Extend `PanacheSearchRepository<T>` and provide the entity class and `EntityManager`:

```java
@ApplicationScoped
public class UserRepository extends PanacheSearchRepository<User> {

    @Inject
    EntityManager em;

    @Override
    protected Class<User> entityClass() {
        return User.class;
    }

    @Override
    protected EntityManager entityManager() {
        return em;
    }
}
```

`PanacheSearchRepository` provides `searchPage(spec, pageNumber, pageSize, sort)` which builds JPA Criteria queries, executes a data query + count query, and returns `PageResource<T>`.

### 4. Service

The service converts frontend input into a paginated query:

```java
@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserSearchMetaData userMetaData;

    public PageResource<User> searchUsers(SortablePageInput input) {
        // 1. Convert search inputs to JPA predicates
        List<SearchCriterium> criteria =
            userMetaData.toSearchCriteria(input.getSearchInputs());
        PanacheSearchSpecification<User> spec =
            new PanacheSearchSpecification<>(criteria);

        // 2. Convert sort columns to Panache Sort
        Sort sort = userMetaData.toSort(input.getSortOrder());

        // 3. Execute paginated query
        return userRepository.searchPage(
            spec, input.getPageNumber(), input.getPageSize(), sort);
    }
}
```

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

Extend `QuarkusPageMapper<Entity, DTO>` for entity→DTO page conversion:

```java
@ApplicationScoped
public class UserDetailsMapper extends QuarkusPageMapper<User, UserDetailsResponse> {

    @Override
    protected UserDetailsResponse mapItem(User user) {
        UserDetailsResponse dto = new UserDetailsResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEnabled(user.isEnabled());
        dto.setRole(user.getRole());
        return dto;
    }
}
```

Call `mapper.map(content, totalElements, totalPages, pageSize, pageNumber)` or build a helper that takes a `PageResource<Entity>` directly.

### 7. REST resource

```java
@Path("/api/admin/users")
@ApplicationScoped
public class AdminUserResource {

    @Inject UserService userService;
    @Inject UserDetailsMapper userDetailsMapper;

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PageResource<UserDetailsResponse> searchUsers(SortablePageInput input) {
        PageResource<User> page = userService.searchUsers(input);
        return userDetailsMapper.map(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getPageSize(),
            page.getPageNumber()
        );
    }
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

| SearchType | Use `textValue` | Use `textValueList` | Use `fromDateValue` + `toDateValue` | `operator` |
|-----------|:-:|:-:|:-:|:-:|
| `TEXT` | ✅ | — | — | — |
| `FUZZY_TEXT` | ✅ | — | — | — |
| `NUMERIC` | ✅ | — | — | — |
| `BOOLEAN` | ✅ | — | — | — |
| `ENUM` | ✅ | — | — | — |
| `MULTI_TEXT` | — | ✅ | — | — |
| `MULTI_ENUM` | — | ✅ | — | — |
| `MULTI_FUZZY` | — | ✅ | — | `AND` / `OR` |
| `MULTI_COLUMN_FUZZY` | ✅ | — | — | — |
| `DATE` | — | — | ✅ | — |

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

Same types as Spring — see [Search types reference](../spring/jpa.md#search-types-reference).

## Sort adapter

`PanacheSortAdapter` converts `List<SortableColumn>` to Panache `Sort`:

```java
Sort sort = PanacheSortAdapter.toSort(input.getSortOrder());
```

This is handled automatically by `AbstractPanacheSearchMetaData.toSort()` — you only need `PanacheSortAdapter` for custom queries outside the search framework.
