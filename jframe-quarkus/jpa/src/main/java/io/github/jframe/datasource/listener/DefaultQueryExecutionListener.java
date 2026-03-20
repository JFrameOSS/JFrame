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

import io.github.jframe.datasource.listener.logger.PrettyQueryEntryCreator;
import io.github.jframe.datasource.listener.logger.SqlStatementLogging;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;

import java.util.List;


/** A listener for logging purposes. */
@Slf4j
public class DefaultQueryExecutionListener extends SLF4JQueryLoggingListener {

    /** Creates a new listener with pretty-printed, multiline SQL formatting. */
    public DefaultQueryExecutionListener() {
        super();
        final PrettyQueryEntryCreator prettyQueryEntryCreator = new PrettyQueryEntryCreator();
        prettyQueryEntryCreator.setMultiline(true);
        this.setQueryLogEntryCreator(prettyQueryEntryCreator);
    }

    @Override
    public void beforeQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        if (SqlStatementLogging.isSuppressed()) {
            log.debug("SQL statement logging is currently suppressed; skipping logging for this query.");
        }
    }
}
