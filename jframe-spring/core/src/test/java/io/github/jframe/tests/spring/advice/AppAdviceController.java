package io.github.jframe.tests.spring.advice;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for application-advice precedence and authentication-exception contract tests. */
@RestController
@RequestMapping("/test/app")
public class AppAdviceController {

    /** Secret exception message that must never leak. */
    public static final String SECRET = "user=admin token=s3cr3t";

    private static final Map<String, Supplier<AuthenticationException>> AUTH = Map.of(
        "credentials-not-found",
        () -> new AuthenticationCredentialsNotFoundException(SECRET),
        "insufficient",
        () -> new InsufficientAuthenticationException(SECRET),
        "bad-credentials",
        () -> new BadCredentialsException(SECRET)
    );

    /** Throws {@link AppDomainException}. */
    @GetMapping("/domain")
    public void domain() {
        throw new AppDomainException(SECRET);
    }

    /** Throws {@link AppHandledHttpException}. */
    @GetMapping("/http")
    public void http() {
        throw new AppHandledHttpException();
    }

    /** Throws an exception no advice of the app handles. */
    @GetMapping("/unhandled")
    public void unhandled() {
        throw new UnsupportedOperationException(SECRET);
    }

    /** Throws the named {@link AuthenticationException} subtype. */
    @GetMapping("/authentication/{kind}")
    public void authentication(@PathVariable("kind") final String kind) {
        throw AUTH.get(kind).get();
    }
}
