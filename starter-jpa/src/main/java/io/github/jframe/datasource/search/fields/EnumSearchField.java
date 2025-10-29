package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

import static java.util.Arrays.stream;

/**
 * Describes a search on an enumeration of fixed values.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnumSearchField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private String value;

    private Class<?> enumClass;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param enumClass  the enum class to search on.
     */
    public EnumSearchField(final String columnName, final Class<?> enumClass) {
        super(columnName, SearchType.ENUM);
        this.enumClass = enumClass;
    }

    /**
     * Retrieve the generic enum value from the enum class.
     *
     * @return the enum value.
     */
    public Object getEnum() {
        return stream(enumClass.getEnumConstants())
            .filter(enumValue -> enumValue.toString().equals(getValue())).findFirst().orElse(null);
    }
}
