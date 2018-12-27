/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.shardingjdbc.jdbc.adapter;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.constant.properties.ShardingPropertiesConstant;
import io.shardingsphere.core.metadata.table.ColumnMetaData;
import io.shardingsphere.core.metadata.table.TableMetaData;
import io.shardingsphere.core.metadata.table.executor.TableMetaDataLoader;
import io.shardingsphere.core.parsing.antlr.sql.segment.definition.column.ColumnDefinitionSegment;
import io.shardingsphere.core.parsing.antlr.sql.segment.definition.column.position.ColumnAfterPositionSegment;
import io.shardingsphere.core.parsing.antlr.sql.segment.definition.column.position.ColumnFirstPositionSegment;
import io.shardingsphere.core.parsing.antlr.sql.segment.definition.column.position.ColumnPositionSegment;
import io.shardingsphere.core.parsing.antlr.sql.statement.ddl.AlterTableStatement;
import io.shardingsphere.core.parsing.antlr.sql.statement.ddl.CreateTableStatement;
import io.shardingsphere.core.routing.SQLRouteResult;
import io.shardingsphere.shardingjdbc.jdbc.adapter.executor.ForceExecuteCallback;
import io.shardingsphere.shardingjdbc.jdbc.adapter.executor.ForceExecuteTemplate;
import io.shardingsphere.shardingjdbc.jdbc.core.connection.ShardingConnection;
import io.shardingsphere.shardingjdbc.jdbc.metadata.JDBCTableMetaDataConnectionManager;
import io.shardingsphere.shardingjdbc.jdbc.unsupported.AbstractUnsupportedOperationStatement;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Adapter for {@code Statement}.
 * 
 * @author zhangliang
 * @author gaohongtao
 * @author yangyi
 */
@RequiredArgsConstructor
public abstract class AbstractStatementAdapter extends AbstractUnsupportedOperationStatement {
    
    private final Class<? extends Statement> targetClass;
    
    private boolean closed;
    
    private boolean poolable;
    
    private int fetchSize;
    
    private final ForceExecuteTemplate<Statement> forceExecuteTemplate = new ForceExecuteTemplate<>();
    
    @SuppressWarnings("unchecked")
    @Override
    public final void close() throws SQLException {
        closed = true;
        try {
            forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
        
                @Override
                public void execute(final Statement statement) throws SQLException {
                    statement.close();
                }
            });
        } finally {
            getRoutedStatements().clear();
        }
    }
    
    @Override
    public final boolean isClosed() {
        return closed;
    }
    
    @Override
    public final boolean isPoolable() {
        return poolable;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void setPoolable(final boolean poolable) throws SQLException {
        this.poolable = poolable;
        recordMethodInvocation(targetClass, "setPoolable", new Class[] {boolean.class}, new Object[] {poolable});
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
            
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.setPoolable(poolable);
            }
        });
    }
    
    @Override
    public final int getFetchSize() {
        return fetchSize;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void setFetchSize(final int rows) throws SQLException {
        this.fetchSize = rows;
        recordMethodInvocation(targetClass, "setFetchSize", new Class[] {int.class}, new Object[] {rows});
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
            
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.setFetchSize(rows);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void setEscapeProcessing(final boolean enable) throws SQLException {
        recordMethodInvocation(targetClass, "setEscapeProcessing", new Class[] {boolean.class}, new Object[] {enable});
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
            
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.setEscapeProcessing(enable);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void cancel() throws SQLException {
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
        
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.cancel();
            }
        });
    }
    
    @Override
    public final int getUpdateCount() throws SQLException {
        long result = 0;
        boolean hasResult = false;
        for (Statement each : getRoutedStatements()) {
            int updateCount = each.getUpdateCount();
            if (updateCount > -1) {
                hasResult = true;
            }
            result += updateCount;
        }
        if (result > Integer.MAX_VALUE) {
            result = Integer.MAX_VALUE;
        }
        return hasResult ? Long.valueOf(result).intValue() : -1;
    }
    
    @Override
    public final SQLWarning getWarnings() {
        return null;
    }
    
    @Override
    public final void clearWarnings() {
    }
    
    @Override
    public final boolean getMoreResults() throws SQLException {
        boolean result = false;
        for (Statement each : getRoutedStatements()) {
            result = each.getMoreResults();
        }
        return result;
    }
    
    @Override
    public final boolean getMoreResults(final int current) {
        return false;
    }
    
    @Override
    public final int getMaxFieldSize() throws SQLException {
        return getRoutedStatements().isEmpty() ? 0 : getRoutedStatements().iterator().next().getMaxFieldSize();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void setMaxFieldSize(final int max) throws SQLException {
        recordMethodInvocation(targetClass, "setMaxFieldSize", new Class[] {int.class}, new Object[] {max});
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
            
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.setMaxFieldSize(max);
            }
        });
    }
    
    // TODO Confirm MaxRows for multiple databases is need special handle. eg: 10 statements maybe MaxRows / 10
    @Override
    public final int getMaxRows() throws SQLException {
        return getRoutedStatements().isEmpty() ? -1 : getRoutedStatements().iterator().next().getMaxRows();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void setMaxRows(final int max) throws SQLException {
        recordMethodInvocation(targetClass, "setMaxRows", new Class[] {int.class}, new Object[] {max});
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
            
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.setMaxRows(max);
            }
        });
    }
    
    @Override
    public final int getQueryTimeout() throws SQLException {
        return getRoutedStatements().isEmpty() ? 0 : getRoutedStatements().iterator().next().getQueryTimeout();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final void setQueryTimeout(final int seconds) throws SQLException {
        recordMethodInvocation(targetClass, "setQueryTimeout", new Class[] {int.class}, new Object[] {seconds});
        forceExecuteTemplate.execute((Collection) getRoutedStatements(), new ForceExecuteCallback<Statement>() {
        
            @Override
            public void execute(final Statement statement) throws SQLException {
                statement.setQueryTimeout(seconds);
            }
        });
    }
    
    protected abstract Collection<? extends Statement> getRoutedStatements();
    
    protected final void refreshTableMetaData(final ShardingConnection connection, final SQLRouteResult routeResult) throws SQLException {
        if (null != routeResult && null != connection && SQLType.DDL == routeResult.getSqlStatement().getType() && !routeResult.getSqlStatement().getTables().isEmpty()) {
            String logicTableName = routeResult.getSqlStatement().getTables().getSingleTableName();
            if (routeResult.getSqlStatement() instanceof CreateTableStatement) {
                createTable(logicTableName, connection, (CreateTableStatement) routeResult.getSqlStatement());
            } else if (routeResult.getSqlStatement() instanceof AlterTableStatement) {
                alterTable(logicTableName, connection, (AlterTableStatement) routeResult.getSqlStatement());
            } else {
                doOther(logicTableName, connection);
            }
        }
    }
    
    private void createTable(final String logicTableName, final ShardingConnection connection, final CreateTableStatement createTableStatement) {
        TableMetaData tableMetaData = new TableMetaData(Lists.transform(createTableStatement.getColumnDefinitions(), new Function<ColumnDefinitionSegment, ColumnMetaData>() {
            
            @Override
            public ColumnMetaData apply(final ColumnDefinitionSegment input) {
                return new ColumnMetaData(input.getColumnName(), input.getDataType(), input.isPrimaryKey());
            }
        }));
        connection.getShardingContext().getMetaData().getTable().put(logicTableName, tableMetaData);
    }
    
    private void alterTable(final String logicTableName, final ShardingConnection connection, final AlterTableStatement alterTableStatement) {
        List<ColumnMetaData> columnMetaDataList = createNewColumnMetaDataList(alterTableStatement, connection.getShardingContext().getMetaData().getTable().get(logicTableName));
        fillAddedColumnDefinitions(alterTableStatement, columnMetaDataList);
        changeColumnDefinitionPositions(alterTableStatement, columnMetaDataList);
        dropColumnDefinitions(alterTableStatement, columnMetaDataList);
        Optional<String> newTableName = alterTableStatement.getNewTableName();
        if (newTableName.isPresent()) {
            connection.getShardingContext().getMetaData().getTable().remove(logicTableName);
            connection.getShardingContext().getMetaData().getTable().put(newTableName.get(), new TableMetaData(columnMetaDataList));
        } else {
            connection.getShardingContext().getMetaData().getTable().put(logicTableName, new TableMetaData(columnMetaDataList));
        }
    }
    
    private List<ColumnMetaData> createNewColumnMetaDataList(final AlterTableStatement alterTableStatement, final TableMetaData oldTableMetaData) {
        List<ColumnMetaData> result = new LinkedList<>();
        for (ColumnMetaData each : oldTableMetaData.getColumnMetaDataList()) {
            String columnName;
            String dataType;
            boolean primaryKey;
            if (alterTableStatement.getModifiedColumnDefinitions().containsKey(each.getColumnName())) {
                ColumnDefinitionSegment modifiedColumnDefinition = alterTableStatement.getModifiedColumnDefinitions().get(each.getColumnName());
                columnName = modifiedColumnDefinition.getColumnName();
                dataType = modifiedColumnDefinition.getDataType();
                primaryKey = !alterTableStatement.isDropPrimaryKey() && modifiedColumnDefinition.isPrimaryKey();
            } else {
                columnName = each.getColumnName();
                dataType = each.getDataType();
                primaryKey = !alterTableStatement.isDropPrimaryKey() && each.isPrimaryKey();
            }
            result.add(new ColumnMetaData(columnName, dataType, primaryKey));
        }
        return result;
    }
    
    private void fillAddedColumnDefinitions(final AlterTableStatement alterTableStatement, final List<ColumnMetaData> columnMetaDataList) {
        for (ColumnDefinitionSegment each : alterTableStatement.getAddedColumnDefinitions()) {
            columnMetaDataList.add(new ColumnMetaData(each.getColumnName(), each.getDataType(), !alterTableStatement.isDropPrimaryKey() && each.isPrimaryKey()));
        }
    }
    
    private void changeColumnDefinitionPositions(final AlterTableStatement alterTableStatement, final List<ColumnMetaData> columnMetaDataList) {
        for (ColumnPositionSegment each : alterTableStatement.getChangedPositionColumns()) {
            if (each instanceof ColumnFirstPositionSegment) {
                adjustFirst(columnMetaDataList, (ColumnFirstPositionSegment) each);
            } else {
                adjustAfter(columnMetaDataList, (ColumnAfterPositionSegment) each);
            }
        }
    }
    
    private void adjustFirst(final List<ColumnMetaData> columnMetaDataList, final ColumnFirstPositionSegment columnFirstPositionSegment) {
        ColumnMetaData firstColumnMetaData = null;
        Iterator<ColumnMetaData> iterator = columnMetaDataList.iterator();
        while (iterator.hasNext()) {
            ColumnMetaData each = iterator.next();
            if (each.getColumnName().equals(columnFirstPositionSegment.getColumnName())) {
                firstColumnMetaData = each;
                iterator.remove();
                break;
            }
        }
        if (null != firstColumnMetaData) {
            columnMetaDataList.add(0, firstColumnMetaData);
        }
    }
    
    private void adjustAfter(final List<ColumnMetaData> columnMetaDataList, final ColumnAfterPositionSegment columnAfterPositionSegment) {
        int afterIndex = -1;
        int adjustColumnIndex = -1;
        for (int i = 0; i < columnMetaDataList.size(); i++) {
            if (columnMetaDataList.get(i).getColumnName().equals(columnAfterPositionSegment.getColumnName())) {
                adjustColumnIndex = i;
            }
            if (columnMetaDataList.get(i).getColumnName().equals(columnAfterPositionSegment.getAfterColumnName())) {
                afterIndex = i;
            }
            if (adjustColumnIndex >= 0 && afterIndex >= 0) {
                break;
            }
        }
        if (adjustColumnIndex >= 0 && afterIndex >= 0 && adjustColumnIndex != afterIndex + 1) {
            ColumnMetaData adjustColumnMetaData = columnMetaDataList.remove(adjustColumnIndex);
            if (afterIndex < adjustColumnIndex) {
                afterIndex = afterIndex + 1;
            }
            columnMetaDataList.add(afterIndex, adjustColumnMetaData);
        }
    }
    
    private void dropColumnDefinitions(final AlterTableStatement alterTableStatement, final List<ColumnMetaData> newColumnMetaData) {
        Iterator<ColumnMetaData> iterator = newColumnMetaData.iterator();
        while (iterator.hasNext()) {
            ColumnMetaData each = iterator.next();
            if (alterTableStatement.getDropColumnNames().contains(each.getColumnName())) {
                iterator.remove();
            }
        }
    }
    
    private void doOther(final String logicTableName, final ShardingConnection connection) throws SQLException {
        TableMetaDataLoader tableMetaDataLoader = new TableMetaDataLoader(connection.getShardingContext().getMetaData().getDataSource(),
                connection.getShardingContext().getExecuteEngine(), new JDBCTableMetaDataConnectionManager(connection.getDataSourceMap()),
                connection.getShardingContext().getShardingProperties().<Integer>getValue(ShardingPropertiesConstant.MAX_CONNECTIONS_SIZE_PER_QUERY), 
                connection.getShardingContext().getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.CHECK_TABLE_METADATA_ENABLED));
        connection.getShardingContext().getMetaData().getTable().put(logicTableName, tableMetaDataLoader.load(logicTableName, connection.getShardingContext().getShardingRule()));
    }
}
