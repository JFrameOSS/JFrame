package io.github.jframe.logging.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/** A definition for a path and method. */
@Getter
@Setter
@NoArgsConstructor
public class PathDefinition {

    private String pattern;

    private String method;

    private final PathMatcher matcher = new AntPathMatcher();

    /** Constructor for the given {@code pattern}. */
    public PathDefinition(final String pattern) {
        this(null, pattern);
    }

    /** Constructor for the given {@code method} and {@code pattern}. */
    public PathDefinition(final String method, final String pattern) {
        this.pattern = pattern;
        this.method = method;
    }

    /** Check whether the method and path matches this definition. */
    public boolean matches(final String method, final String path) {
        boolean matches = true;
        if (this.method != null) {
            matches = this.method.equals(method);
        }

        return matches && matcher.match(this.pattern, path);
    }
}
