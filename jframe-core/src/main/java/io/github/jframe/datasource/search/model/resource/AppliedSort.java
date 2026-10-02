package io.github.jframe.datasource.search.model.resource;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The sort that was actually applied to a page response.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AppliedSort {

    private String field;

    private String direction;

    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private boolean defaultApplied;

    /** Returns {@code true} when the default sort was applied. */
    @JsonProperty("isDefault")
    public boolean isDefault() {
        return defaultApplied;
    }

    /** Sets whether the default sort was applied. */
    @JsonProperty("isDefault")
    public void setDefault(final boolean value) {
        this.defaultApplied = value;
    }
}
