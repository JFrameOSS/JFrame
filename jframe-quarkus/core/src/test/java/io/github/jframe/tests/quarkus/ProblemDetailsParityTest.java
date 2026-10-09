package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.factory.DefaultErrorResponseFactory;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.jframe.validation.ValidationResult;
import io.github.support.ProblemJson;
import io.github.support.fixtures.TestApiError;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import jakarta.enterprise.inject.Instance;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jframe.exception.factory.ProblemDetailsFixture.aBuilder;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.aRequest;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.builtInEnrichersPlus;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cross-runtime parity, application root path, configured type base URI and bean-validation for the Quarkus adapter.
 */
@DisplayName("Quarkus Integration - Problem Details cross-runtime parity")
class ProblemDetailsParityTest {

    private static final String ROOT_PATH = "/app";
    private static final String CUSTOM_TYPE_BASE_URI = "https://errors.example.com/";

    private static final String SECRET = "jdbc:postgresql://db/secret";

    @NotNull
    private static Object notNullHolder;

    static Stream<Arguments> scenarios() {
        final ValidationResult validation = new ValidationResult();
        validation.rejectValue("name", "name.required");
        validation.rejectValue("email", "email.required");
        return Stream.of(
            Arguments.of(
                new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT)),
                "/test/business",
                "business-http-exception"
            ),
            Arguments.of(new ValidationException(validation), "/test/validation-error", "validation-error"),
            Arguments.of(
                new RateLimitExceededException(100, 0, OffsetDateTime.parse("2030-01-01T12:00:00Z")),
                "/test/rate-limit",
                "rate-limit"
            ),
            Arguments.of(new InvalidSortException("password", List.of("name", "createdAt")), "/test/invalid-sort", "invalid-sort"),
            Arguments.of(new IllegalStateException("jdbc:postgresql://db/secret"), "/test/unexpected", "unhandled-500"),
            Arguments.of(new NotFoundException(), "/test/does-not-exist", "framework-404"),
            Arguments.of(new NotAllowedException("GET"), "/test/bad-request", "framework-405"),
            Arguments.of(new CompletionException(new NotFoundException()), "/test/unexpected", "unhandled-500"),
            Arguments.of(new ExecutionException(new NotFoundException()), "/test/unexpected", "unhandled-500"),
            Arguments.of(
                new WebApplicationException(new IllegalArgumentException(SECRET), Response.Status.BAD_REQUEST),
                "/test/json-body",
                "unreadable-body"
            )
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("scenarios")
    @DisplayName("Should render body equal to the shared expected body")
    void shouldRenderSharedExpectedBody(final Throwable exception, final String path, final String expected) {
        // Given: No type base URI configured, built-in enrichers, runtime mapper selection
        final ErrorResponseEntityBuilder builder = aBuilder(builtInEnrichersPlus());

        // When: Mapping the exception
        final Response response = QuarkusProblems.map(exception, builder, aRequest(path));

        // Then: RFC 9457 conformant and identical to the Spring expectation
        assertThat(response.getMediaType().toString(), is(ProblemJson.PROBLEM_JSON));
        ProblemJson.assertMatchesExpected(QuarkusProblems.conformantJson(response), expected);
    }

    @Test
    @DisplayName("Should include application root path in instance")
    void shouldIncludeRootPathInInstanceWhenDeployedUnderRootPath() {
        // Given: A request under the application root path (JAX-RS path is relative to it)
        final ContainerRequestContext request = aRootedRequest("/test/business");
        final HttpException exception =
            new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));

        // When: Mapping the exception
        final Response response = QuarkusProblems.map(exception, aBuilder(builtInEnrichersPlus()), request);

        // Then: instance is the full path the client requested
        assertThat(ProblemJson.parse(QuarkusProblems.conformantJson(response)), hasEntry("instance", ROOT_PATH + "/test/business"));
    }

    @Test
    @DisplayName("Should use configured jframe.exception.type-base-uri in type")
    void shouldUseConfiguredTypeBaseUri() {
        // Given: jframe.exception.type-base-uri configured and the CDI-constructed builder
        final ErrorResponseEntityBuilder builder = withConfig(
            Map.of(ErrorResponseEntityBuilder.TYPE_BASE_URI_PROPERTY, CUSTOM_TYPE_BASE_URI),
            () -> new ErrorResponseEntityBuilder(new DefaultErrorResponseFactory(), anInstance(builtInEnrichersPlus()))
        );
        final HttpException exception =
            new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));

        // When: Mapping an exception
        final Response response = QuarkusProblems.map(exception, builder, aRequest("/test/business"));

        // Then: type uses the configured base URI
        ProblemJson.assertMatchesExpected(QuarkusProblems.conformantJson(response), "business-http-exception", CUSTOM_TYPE_BASE_URI);
    }

    @Test
    @DisplayName("Should percent-encode error code in type when base URI configured")
    void shouldPercentEncodeErrorCodeInConfiguredType() {
        // Given: Configured base URI and an unsafe error code
        final ErrorResponseEntityBuilder builder = withConfig(
            Map.of(ErrorResponseEntityBuilder.TYPE_BASE_URI_PROPERTY, CUSTOM_TYPE_BASE_URI),
            () -> new ErrorResponseEntityBuilder(new DefaultErrorResponseFactory(), anInstance(builtInEnrichersPlus()))
        );
        final HttpException exception = new HttpException(new TestApiError("BAD CODE/1", "Unsafe code", Response.Status.BAD_REQUEST));

        // When: Mapping it
        final Response response = QuarkusProblems.map(exception, builder, aRequest("/test/unsafe-code"));

        // Then: type percent-encodes the code
        assertThat(ProblemJson.parse(QuarkusProblems.conformantJson(response)), hasEntry("type", CUSTOM_TYPE_BASE_URI + "BAD%20CODE%2F1"));
    }

    @Test
    @DisplayName("Should omit type when jframe.exception.type-base-uri is blank")
    void shouldOmitTypeWhenTypeBaseUriBlank() {
        // Given: Blank base URI property
        final ErrorResponseEntityBuilder builder = withConfig(
            Map.of(ErrorResponseEntityBuilder.TYPE_BASE_URI_PROPERTY, " "),
            () -> new ErrorResponseEntityBuilder(new DefaultErrorResponseFactory(), anInstance(builtInEnrichersPlus()))
        );
        final HttpException exception =
            new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));

        // When: Mapping it
        final Response response = QuarkusProblems.map(exception, builder, aRequest("/test/business"));

        // Then: Same as not configured
        ProblemJson.assertMatchesExpected(QuarkusProblems.conformantJson(response), "business-http-exception");
    }

    @Test
    @DisplayName("Should render ConstraintViolationException as 400 VALIDATION_ERROR with errors")
    void shouldRenderConstraintViolationAsValidationError() throws Exception {
        // Given: A bean-validation failure on field 'name'
        final ConstraintViolationException exception = new ConstraintViolationException(Set.of(aViolation("name")));

        // When: Mapping it through runtime mapper selection
        final Response response = QuarkusProblems.map(
            exception,
            aBuilder(builtInEnrichersPlus()),
            aRequest("/test/bean-validation")
        );
        final Map<String, Object> body = ProblemJson.parse(QuarkusProblems.conformantJson(response));

        // Then: 400 VALIDATION_ERROR listing the field
        assertThat(response.getStatus(), is(400));
        assertThat(body, hasEntry("errorCode", "VALIDATION_ERROR"));
        assertThat(body, not(hasKey("type")));
        final List<?> errors = (List<?>) body.get("errors");
        assertThat(errors, hasSize(1));
        assertThat(((Map<?, ?>) errors.get(0)).get("field"), is("name"));
    }

    @Test
    @DisplayName("Should render return-value ConstraintViolationException as generic 500 without errors")
    void shouldRenderReturnValueViolationAsInternalServerError() throws Exception {
        // Given: A bean-validation failure on a method return value
        final ConstraintViolationException exception = new ConstraintViolationException(Set.of(aReturnValueViolation()));

        // When: Mapping it through runtime mapper selection
        final Response response = QuarkusProblems.map(
            exception,
            aBuilder(builtInEnrichersPlus()),
            aRequest("/test/bean-validation")
        );
        final String json = QuarkusProblems.conformantJson(response);
        final Map<String, Object> body = ProblemJson.parse(json);

        // Then: 500 INTERNAL_SERVER_ERROR, generic detail, no violation details
        assertThat(response.getStatus(), is(500));
        assertThat(body, hasEntry("errorCode", "INTERNAL_SERVER_ERROR"));
        assertThat(body, hasEntry("detail", "Internal server error"));
        assertThat(body, not(hasKey("errors")));
        assertThat(json, not(containsString("must not be null")));
        assertThat(json, not(containsString("result")));
    }

    @Test
    @DisplayName("Should keep Allow header on 405 NotAllowedException")
    void shouldKeepAllowHeaderOnNotAllowed() {
        // Given: A 405 for a resource allowing GET and POST
        final NotAllowedException exception = new NotAllowedException(
            "Method not allowed",
            "GET",
            new String[] {
                "POST"
            }
        );

        // When: Mapping it through runtime mapper selection
        final Response response = QuarkusProblems.map(exception, aBuilder(builtInEnrichersPlus()), aRequest("/test/bad-request"));

        // Then: 405 with Allow header preserved
        assertThat(response.getStatus(), is(405));
        assertThat(response.getHeaderString(HttpHeaders.ALLOW), allOf(containsString("GET"), containsString("POST")));
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<Object> aReturnValueViolation() throws NoSuchFieldException {
        final ConstraintViolation<Object> violation = aViolation("getOrder.<return value>.result");
        final Path path = violation.getPropertyPath();
        final Path.Node method = mock(Path.Node.class);
        final Path.Node returnValue = mock(Path.Node.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);
        when(returnValue.getKind()).thenReturn(ElementKind.RETURN_VALUE);
        when(path.iterator()).thenAnswer(invocation -> List.of(method, returnValue).iterator());
        when(path.spliterator()).thenAnswer(invocation -> List.of(method, returnValue).spliterator());
        return violation;
    }

    private static ContainerRequestContext aRootedRequest(final String path) {
        final ContainerRequestContext context = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080" + ROOT_PATH + path));
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080" + ROOT_PATH + "/"));
        when(uriInfo.getPath()).thenReturn(path);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getMethod()).thenReturn("GET");
        return context;
    }

    @SuppressWarnings("unchecked")
    private static Instance<ErrorResponseEnricher> anInstance(final List<ErrorResponseEnricher> enrichers) {
        final Instance<ErrorResponseEnricher> instance = mock(Instance.class);
        when(instance.stream()).thenAnswer(invocation -> enrichers.stream());
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<Object> aViolation(final String field) throws NoSuchFieldException {
        final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        final Path path = mock(Path.class);
        final ConstraintDescriptor<Annotation> descriptor = mock(ConstraintDescriptor.class);
        final Annotation notNull = ProblemDetailsParityTest.class.getDeclaredField("notNullHolder").getAnnotation(NotNull.class);
        when(path.toString()).thenReturn(field);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");
        doReturn(descriptor).when(violation).getConstraintDescriptor();
        when(descriptor.getAnnotation()).thenReturn(notNull);
        return violation;
    }

    /** Runs the action with the properties as system config, isolated via a fresh context class loader. */
    private static <T> T withConfig(final Map<String, String> properties, final Supplier<T> action) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        properties.forEach(System::setProperty);
        thread.setContextClassLoader(new ClassLoader(original) {
        });
        try {
            return action.get();
        } finally {
            thread.setContextClassLoader(original);
            properties.keySet().forEach(System::clearProperty);
        }
    }
}
