package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

import static java.util.Arrays.stream;

/**
 * Indicates the search criterium is an enum field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnumField extends SearchCriterium {

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
    public EnumField(final String columnName, final Class<?> enumClass, final String value) {
        super(columnName, SearchType.ENUM);
        this.enumClass = enumClass;
        this.value = StringUtils.isNotBlank(value) ? value : null;
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
