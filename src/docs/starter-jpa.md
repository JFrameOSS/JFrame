# starter-jpa

JPA enhancements for database operations, dynamic search, pagination, and query monitoring.

## Location

```
starter-jpa/src/main/java/io/github/jframe/datasource/
├── config/                 # Datasource configuration
│   └── DatasourceProxyConfiguration.java
├── mapper/                 # Result mappers
│   └── PageMapper.java
└── model/search/           # Search and pagination
    ├── input/             # SearchInput base class
    ├── fields/            # SearchCriterium types (Numeric, Text, Enum, etc.)
    ├── resource/          # PageResource response
    └── JpaSearchSpecification.java
```

## Components

### Search Framework

Dynamic JPA specification building with pagination and sorting.

**Key Classes:**
- `SearchInput` - Base class for search requests with pagination
- `JpaSearchSpecification` - Generic specification builder
- `PageResource` - Standardized paginated response
- `PageMapper` - Converts JPA Page to PageResource
- `SearchCriteriumFactory` - Factory for creating criteria from inputs

**Search Field Types & Features:**

| Field Type | Description | Inverse Filter Support (e.g. `!value`) |
|------------|-------------|----------------------------------------|
| `TextField` | Exact text match | ✅ Yes (`!value` -> `NOT EQUAL`) |
| `NumericField` | Exact number match | ✅ Yes (`!123` -> `NOT EQUAL`) |
| `EnumField` | Exact enum match | ✅ Yes (`!ACTIVE` -> `NOT EQUAL`) |
| `MultiTextField` | Match any string in list | ✅ Yes (First item `!val` -> `NOT IN (...)`) |
| `MultiEnumField` | Match any enum in list | ✅ Yes (First item `!val` -> `NOT IN (...)`) |
| `BooleanField` | Boolean value match | ❌ No |
| `DateField` | Date range search | ❌ No |
| `FuzzyTextField` | Like search (`%val%`) | ❌ No |
| `MultiFuzzyField` | Multiple like searches | ❌ No |

**Inverse Filtering:**
You can prefix values with `!` to negate the filter condition.
- Single value fields (`TextField`, `NumericField`, `EnumField`) become `NOT EQUAL`.
- Multi-value fields (`MultiTextField`, `MultiEnumField`) become `NOT IN (...)` if the **first** element starts with `!`.

**Example:**
```java
// Search input
public class UserSearchInput extends SearchInput {
    private String firstName;
    private String status; // Supports "!INACTIVE"
    private List<String> roles; // Supports ["!GUEST", "!TEMP"] -> NOT IN (GUEST, TEMP)

    @Override
    public List<SortableColumn> getSortableColumns() {
        return Arrays.asList(
            SortableColumn.of("firstName", "First Name"),
            SortableColumn.of("email", "Email")
        );
    }
}

// Service
public PageResource<UserDto> search(UserSearchInput input) {
    List<SearchCriterium> criteria = new ArrayList<>();
    
    if (input.getFirstName() != null) {
        criteria.add(new FuzzyTextField("firstName", input.getFirstName()));
    }
    if (input.getStatus() != null) {
        // Automatically handles "!INACTIVE" as NOT EQUAL
        criteria.add(new EnumField("status", UserStatus.class, input.getStatus()));
    }
    if (input.getRoles() != null) {
        // Automatically handles ["!GUEST"] as NOT IN
        criteria.add(new MultiEnumField("role", Role.class, input.getRoles()));
    }

    Specification<User> spec = new JpaSearchSpecification<>(criteria);
    Page<User> users = repository.findAll(spec, input.toPageable());
    
    return PageMapper.toPageResource(users, UserResource::from);
}
```

### Pagination

**PageResource Structure:**
```java
{
  "content": [...],
  "page": {
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "number": 0
  },
  "sortableColumns": [...]
}
```

**Usage:**
```java
@GetMapping("/search")
public ResponseEntity<PageResource<UserDto>> search(UserSearchInput input) {
    return ResponseEntity.ok(userService.search(input));
}
```

### Database Proxy

Automatic query logging and performance monitoring via datasource-proxy.

**Features:**
- Query execution time logging
- Parameter value logging
- Query result counting
- Performance metrics collection

**Location:** `io.github.jframe.datasource.config.DatasourceProxyConfiguration`

## Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot Data JPA | JPA functionality |
| Spring Boot JDBC | Database connectivity |
| datasource-proxy | Query logging and monitoring |
| Commons IO/Lang3/Collections | Utilities |
| starter-core | Foundation (ApplicationProperties, utilities) |

## Configuration

```yaml
jframe:
  datasource:
    proxy:
      enabled: true
      log-level: INFO
      slow-query-threshold: 1000ms

  search:
    default-page-size: 20
    max-page-size: 100
    case-sensitive: false
```

## Complete Example

```java
// Entity
@Entity
public class User {
    @Id private Long id;
    private String firstName;
    private String email;
    private UserStatus status;
    private LocalDateTime createdAt;
}

// Repository
public interface UserRepository extends
    JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {}

// Search Input
public class UserSearchInput extends SearchInput {
    private String firstName;
    private String email;
    private String status;
    private LocalDateTime createdAfter;

    @Override
    public List<SortableColumn> getSortableColumns() {
        return Arrays.asList(
            SortableColumn.of("firstName", "First Name"),
            SortableColumn.of("email", "Email"),
            SortableColumn.of("createdAt", "Created Date")
        );
    }
}

// Service
@Service
public class UserService {
    @Autowired private UserRepository repository;

    public PageResource<UserDto> search(UserSearchInput input) {
        List<SearchCriterium> criteria = new ArrayList<>();
        
        // Add criteria
        criteria.add(new FuzzyTextField("firstName", input.getFirstName()));
        criteria.add(new TextField("email", input.getEmail()));
        criteria.add(new EnumField("status", UserStatus.class, input.getStatus()));
        
        if (input.getCreatedAfter() != null) {
            criteria.add(new DateField("createdAt", input.getCreatedAfter().toString(), null));
        }

        Specification<User> spec = new JpaSearchSpecification<>(criteria);
        Page<User> page = repository.findAll(spec, input.toPageable());
        
        return PageMapper.toPageResource(page, UserDto::from);
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired private UserService service;

    @GetMapping("/search")
    public ResponseEntity<PageResource<UserDto>> search(@Valid UserSearchInput input) {
        return ResponseEntity.ok(service.search(input));
    }
}
```

## Best Practices

1. **Extend SearchInput** - Always extend for consistent pagination
2. **Use JPA Metamodel** - Type-safe property references (`User_`)
3. **Implement getSortableColumns()** - Define available sorting options
4. **Add Database Indexes** - Index commonly searched fields
5. **Monitor Slow Queries** - Use datasource proxy for performance analysis
6. **Use Inverse Filtering** - Leverage the `!` prefix for exclusions in text, numeric, and enum fields.

## Integration

### With starter-core
- Uses `ObjectMappers` for JSON processing
- Leverages shared MapStruct configurations
- Inherits `ApplicationProperties`

### With starter-otlp
- Automatic database operation tracing
- Query performance metrics
- Span creation for repository methods

## See Also

- [starter-core](./starter-core.md)
- [starter-otlp](./starter-otlp.md)
