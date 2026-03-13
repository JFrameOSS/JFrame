package io.github.jframe.util;

import io.github.jframe.exception.core.ResourceNotFoundException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for loading resources.
 */
@Slf4j
@UtilityClass
@SuppressWarnings("MultipleStringLiterals")
public class ResourceLoaderUtil {

    /**
     * Get the file from the resource folder via a classloader.
     *
     * @param path the resource path.
     * @return the resource file.
     */
    public static File getResourceFile(final String path) {
        try {
            final Resource resource = getResource(path);
            final File file = resource.getFile();
            if (!file.exists()) {
                throw new ResourceNotFoundException("Resource does not exist: " + file.getAbsolutePath());
            }
            return file;
        } catch (final IOException exception) {
            log.error("Error while retrieving the resource file.", exception);
            throw new ResourceNotFoundException(exception);
        }
    }

    /**
     * Get the resource as string.
     *
     * @param path the resource path.
     * @return the resource as string.
     */
    public static String getResourceAsString(final String path) {
        final Resource resource = getResource(path);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (final IOException exception) {
            log.error("Cannot load resource as string.", exception);
            throw new ResourceNotFoundException(exception);
        }
    }

    /**
     * Returns the resource on the classpath.
     *
     * @param path the resource path.
     * @return The resource on the classpath.
     */
    public static Resource getResource(final String path) {
        final String classPath = getClassPath(path);
        final ResourceLoader resourceLoader = new DefaultResourceLoader();
        final Resource resource = resourceLoader.getResource(classPath);

        if (!resource.exists()) {
            throw new ResourceNotFoundException("Resource does not exist: " + classPath);
        }
        return resource;
    }

    private static String getClassPath(final String path) {
        return path.contains("resources")
            ? path.split("resources")[1]
            : path;
    }
}
