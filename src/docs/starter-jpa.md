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

**Search Field Types:**
- `DateSearchField` - Date range searching
- `MultiWordSearchField` - Text-based multi-word search
- `DropdownStringSearchField` - Dropdown selection
- `MultipleSelectSearchField` - Multi-select filtering

**Example:**
```java
// Search input
public class UserSearchInput extends SearchInput {
    private String firstName;
    private String email;

    @Override
    public List<SortableColumn> getSortableColumns() {
        return Arrays.asList(
            SortableColumn.of("firstName", "First Name"),
            SortableColumn.of("email", "Email")
        );
    }
}

// Service
Specification<User> spec = JpaSearchSpecification.<User>builder()
    .when(input.getFirstName())
        .like(User_.firstName)
    .when(input.getEmail())
        .equal(User_.email)
    .build();

Page<User> users = repository.findAll(spec, input.toPageable());
return PageMapper.toPageResource(users, UserResource::from);
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
        Specification<User> spec = JpaSearchSpecification.<User>builder()
            .when(input.getFirstName())
                .like(User_.firstName)
            .when(input.getEmail())
                .equal(User_.email)
            .when(input.getCreatedAfter())
                .greaterThanOrEqual(User_.createdAt)
            .build();

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