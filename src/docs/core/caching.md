# Request-Scoped Caching

Generic request-scoped cache infrastructure for eliminating duplicate database queries within a single HTTP request.

## Overview

The `RequestScopedCache<K, V>` abstract class provides a thread-safe, request-scoped cache that automatically discards cached entities when the HTTP request completes. This eliminates the N+1 query problem and reduces database load without manual cache management.

## Location

```
starter-core/src/main/java/io/github/jframe/cache/
└── RequestScopedCache.java    # Abstract base class for entity caching
```

## Key Concepts

### Request Scope Lifecycle

- Cache is created when first accessed during a request
- All cached entities are automatically discarded when request ends
- No explicit cleanup or invalidation required
- Thread-safe using `ConcurrentHashMap`

### Cache Implementation Pattern

Extend `RequestScopedCache` with `@Component` and `@RequestScope`:

```java
@Component
@RequestScope
public class UserCache extends RequestScopedCache<Long, User> {
    @Override
    protected Long getId(User entity) {
        return entity.getId();
    }
}
```

For UUID-based entities:

```java
@Component
@RequestScope
public class OrderCache extends RequestScopedCache<UUID, Order> {
    @Override
    protected UUID getId(Order entity) {
        return entity.getId();
    }
}
```

## API Reference

### Core Methods

| Method | Description |
|--------|-------------|
| `put(V entity)` | Store a single entity |
| `putAll(Collection<V>)` | Store multiple entities |
| `get(K id)` | Retrieve entity by ID (returns `Optional`) |
| `getOrLoad(K id, Function<K, Optional<V>>)` | Get from cache or load if missing |
| `getAll()` | Get all cached entities |
| `getAll(Collection<K>)` | Get cached entities for specific IDs |
| `getAllOrLoad(Collection<K>, Function<...>)` | Batch get/load for multiple IDs |
| `contains(K id)` | Check if entity is cached |
| `size()` | Get cache size |
| `remove(K id)` | Remove entity from cache |
| `clear()` | Remove all entries |

### Abstract Methods

| Method | Description |
|--------|-------------|
| `getId(V entity)` | Extract identifier from entity (must implement) |

## Usage Patterns

### Single Entity Loading

Load entity from cache or database:

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserCache userCache;
    private final UserRepository userRepository;

    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        // Cache hit or load from DB
        User user = userCache.getOrLoad(order.getUserId(), 
            id -> userRepository.findById(id))
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return mapToDto(order, user);
    }
}
```

### Batch Loading

Efficiently load multiple entities, only querying for uncached ones:

```java
public List<OrderDto> getOrders(List<Long> orderIds) {
    List<Order> orders = orderRepository.findAllById(orderIds);
    
    // Collect all user IDs needed
    Set<Long> userIds = orders.stream()
        .map(Order::getUserId)
        .collect(Collectors.toSet());
    
    // Batch load: only queries DB for uncached users
    Map<Long, User> users = userCache.getAllOrLoad(userIds, 
        ids -> userRepository.findAllById(ids));
    
    return orders.stream()
        .map(order -> mapToDto(order, users.get(order.getUserId())))
        .toList();
}
```

### Pre-warming Cache

Populate cache before processing:

```java
public void processOrders(List<Order> orders) {
    // Pre-fetch all users in single query
    Set<Long> userIds = orders.stream()
        .map(Order::getUserId)
        .collect(Collectors.toSet());
    
    List<User> users = userRepository.findAllById(userIds);
    userCache.putAll(users);
    
    // Now process orders - all user lookups are cache hits
    for (Order order : orders) {
        User user = userCache.get(order.getUserId()).orElseThrow();
        processOrder(order, user);
    }
}
```

### Conditional Caching

Cache entities after conditional loading:

```java
public User findOrCreateUser(String email) {
    // Check cache first
    Optional<User> cached = userCache.getAll().values().stream()
        .filter(u -> u.getEmail().equals(email))
        .findFirst();
    
    if (cached.isPresent()) {
        return cached.get();
    }
    
    // Load or create
    User user = userRepository.findByEmail(email)
        .orElseGet(() -> userRepository.save(new User(email)));
    
    userCache.put(user);
    return user;
}
```

## Best Practices

### Do

- Use for entities accessed multiple times per request
- Prefer batch loading (`getAllOrLoad`) over multiple single loads
- Pre-warm cache when you know all needed entities upfront
- Use meaningful cache class names (e.g., `UserCache`, `ProductCache`)

### Don't

- Don't use for write-heavy entities (cache may become stale within request)
- Don't store sensitive data that shouldn't persist in memory
- Don't use for very large datasets (memory constraints)
- Don't call `clear()` manually (request scope handles cleanup)

## Thread Safety

The cache uses `ConcurrentHashMap` internally and is safe for concurrent access within the same request. The `getOrLoad` method uses atomic `computeIfAbsent` to prevent duplicate loading.

## Performance Considerations

### Memory

- Cache lifetime is limited to request duration
- Consider entity size when caching large objects
- For large result sets, consider pagination instead of full caching

### Query Optimization

- `getAllOrLoad` executes a single batch query for missing entities
- Reduces N+1 queries to 1+1 queries (initial fetch + batch load)
- Useful in loops that would otherwise query per iteration

## Example: Complete Service

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerCache customerCache;
    private final CustomerRepository customerRepository;
    private final ProductCache productCache;
    private final ProductRepository productRepository;

    public List<InvoiceDto> getInvoicesForPeriod(LocalDate from, LocalDate to) {
        List<Invoice> invoices = invoiceRepository.findByDateBetween(from, to);
        
        // Collect all IDs
        Set<Long> customerIds = invoices.stream()
            .map(Invoice::getCustomerId)
            .collect(Collectors.toSet());
        
        Set<Long> productIds = invoices.stream()
            .flatMap(inv -> inv.getLineItems().stream())
            .map(LineItem::getProductId)
            .collect(Collectors.toSet());
        
        // Batch load all related entities (2 queries total)
        Map<Long, Customer> customers = customerCache.getAllOrLoad(
            customerIds, ids -> customerRepository.findAllById(ids));
        
        Map<Long, Product> products = productCache.getAllOrLoad(
            productIds, ids -> productRepository.findAllById(ids));
        
        // Map to DTOs using cached entities
        return invoices.stream()
            .map(inv -> mapToDto(inv, 
                customers.get(inv.getCustomerId()),
                products))
            .toList();
    }
}
```

## See Also

- [starter-core](../starter-core.md)
- [Exception Handling](./exception-handling.md)
- [Validation Framework](./validation.md)
