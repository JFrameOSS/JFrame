package io.github.jframe.datasource.logging;

import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

import java.util.List;

import static io.github.jframe.util.IndentUtil.indent;
import static io.github.jframe.util.constants.Constants.Characters.SYSTEM_NEW_LINE;
import static net.ttddyy.dsproxy.proxy.ParameterSetOperation.isSetNullParameterOperation;

/**
 * A query log entry creator that produces detailed output including SQL query text
 * and all bound parameter values.
 *
 * <p>Output format:
 * <pre>
 * SELECT * FROM users WHERE id = ?
 *
 * parameters:
 * '42',
 * 'John'
 * </pre>
 */
public class DetailedQueryEntryCreator extends DefaultQueryLogEntryCreator {

    /**
     * Formats a single query with its bound parameters.
     *
     * @param queryInfo the query info containing SQL and parameters
     * @return formatted string with query and parameters
     */
    public String formatQueryWithParameters(final QueryInfo queryInfo) {
        final StringBuilder builder = new StringBuilder(128);
        builder.append(queryInfo.getQuery());

        boolean parameterHeaderAppended = false;
        for (final List<ParameterSetOperation> parameterSetOperations : queryInfo.getParametersList()) {
            for (final ParameterSetOperation parameterSetOperation : parameterSetOperations) {
                if (!parameterHeaderAppended) {
                    builder
                        .append(SYSTEM_NEW_LINE)
                        .append(SYSTEM_NEW_LINE)
                        .append("parameters:")
                        .append(SYSTEM_NEW_LINE);
                    parameterHeaderAppended = true;
                }
                if (isSetNullParameterOperation(parameterSetOperation)) {
                    builder.append("null");
                } else {
                    final Object[] args = parameterSetOperation.getArgs();
                    builder.append('\'').append(args[1]).append('\'');
                }
                builder.append(',').append(SYSTEM_NEW_LINE);
            }
        }

        String value = builder.toString();
        if (value.endsWith("," + SYSTEM_NEW_LINE)) {
            value = value.substring(0, value.length() - 1 - SYSTEM_NEW_LINE.length());
        }
        return indent(value);
    }
}
