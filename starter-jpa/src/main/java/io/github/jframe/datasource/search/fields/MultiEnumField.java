package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import static java.util.Arrays.stream;

/**
 * Indicates the search criterium is an enum field for searching multiple enum values.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultiEnumField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private List<String> values;

    private Class<?> enumClass;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.A
     * @param enumClass  the enum class to search on.
     */
    public MultiEnumField(final String columnName, final Class<?> enumClass, final List<String> values) {
        super(columnName, SearchType.MULTI_ENUM);
        this.enumClass = enumClass;
        if (CollectionUtils.isNotEmpty(values)) {
            if (values.get(0).startsWith("!")) {
                setInverse(true);
                this.values = values.stream()
                    .map(val -> val.startsWith("!") ? val.substring(1) : val)
                    .toList();
            } else {
                this.values = values;
            }
        } else {
            this.values = Collections.emptyList();
        }
    }

    /**
     * Retrieve the generic enum value from the enum class.
     *
     * @return the enum value.
     */
    public List<?> getEnums() {
        return stream(enumClass.getEnumConstants())
            .filter(enumValue -> getValues().contains(enumValue.toString())).toList();
    }
}
