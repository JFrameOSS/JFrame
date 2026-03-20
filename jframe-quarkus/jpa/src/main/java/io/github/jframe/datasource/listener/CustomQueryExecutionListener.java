/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jframe.datasource.listener;

import io.github.jframe.datasource.listener.logger.SqlStatementLogging;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

import java.util.List;

import static io.github.jframe.util.IndentUtil.indent;
import static io.github.jframe.util.constants.Constants.Characters.SYSTEM_NEW_LINE;
import static net.ttddyy.dsproxy.proxy.ParameterSetOperation.isSetNullParameterOperation;

/** A listener for logging purposes. */
@Slf4j
public class CustomQueryExecutionListener extends SLF4JQueryLoggingListener {

    @Override
    public void beforeQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        if (SqlStatementLogging.isSuppressed()) {
            return;
        }

        final QueryInfo queryInfo = queryInfoList.getFirst();
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
        log.info("Executing query: {}{}", SYSTEM_NEW_LINE, indent(value));
    }

    @Override
    public void afterQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        if (SqlStatementLogging.isSuppressed()) {
            log.debug("SQL statement logging is currently suppressed; skipping logging for this query.");
        }
    }
}
