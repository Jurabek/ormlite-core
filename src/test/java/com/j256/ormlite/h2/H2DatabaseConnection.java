package com.j256.ormlite.h2;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.GenericRowMapper;
import com.j256.ormlite.stmt.StatementBuilder.StatementType;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.DatabaseResults;
import com.j256.ormlite.support.GeneratedKeyHolder;

/**
 * H2 connection. Unfortunately, a good bit of this is copied from the JdbcDatabaseConnection.
 * 
 * @author graywatson
 */
public class H2DatabaseConnection implements DatabaseConnection {

	private static final String JDBC_META_TABLE_NAME_COLUMN = "TABLE_NAME";

	private Connection connection;
	private static GenericRowMapper<Long> longWrapper = new OneLongWrapper();

	public H2DatabaseConnection(Connection connection) {
		this.connection = connection;
	}

	public boolean isAutoCommitSupported() throws SQLException {
		return true;
	}

	public boolean getAutoCommit() throws SQLException {
		return connection.getAutoCommit();
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		connection.setAutoCommit(autoCommit);
	}

	public Savepoint setSavePoint(String name) throws SQLException {
		return connection.setSavepoint(name);
	}

	public void commit(Savepoint savePoint) throws SQLException {
		connection.releaseSavepoint(savePoint);
	}

	public void rollback(Savepoint savePoint) throws SQLException {
		connection.rollback(savePoint);
	}

	public CompiledStatement compileStatement(String statement, StatementType type, FieldType[] argFieldTypes) throws SQLException {
		return new H2CompiledStatement(connection.prepareStatement(statement, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY));
	}

	public int insert(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		return update(statement, args, argFieldTypes);
	}

	public int insert(String statement, Object[] args, FieldType[] argFieldTypes, GeneratedKeyHolder keyHolder)
			throws SQLException {
		PreparedStatement stmt = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
		statementSetArgs(stmt, args, argFieldTypes);
		int rowN = stmt.executeUpdate();
		ResultSet resultSet = stmt.getGeneratedKeys();
		ResultSetMetaData metaData = resultSet.getMetaData();
		int colN = metaData.getColumnCount();
		while (resultSet.next()) {
			for (int colC = 1; colC <= colN; colC++) {
				// get the id column data so we can pass it back to the caller thru the keyHolder
				Number id = getIdColumnData(resultSet, metaData, colC);
				keyHolder.addKey(id);
			}
		}
		return rowN;
	}

	public int update(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement(statement);
		statementSetArgs(stmt, args, argFieldTypes);
		return stmt.executeUpdate();
	}

	public int delete(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		return update(statement, args, argFieldTypes);
	}

	public <T> Object queryForOne(String statement, Object[] args, FieldType[] argFieldTypes,
			GenericRowMapper<T> rowMapper) throws SQLException {
		PreparedStatement stmt =
				connection.prepareStatement(statement, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		statementSetArgs(stmt, args, argFieldTypes);
		DatabaseResults results = new H2DatabaseResults(stmt.executeQuery());
		if (!results.next()) {
			// no results at all
			return null;
		}
		T first = rowMapper.mapRow(results);
		if (results.next()) {
			return MORE_THAN_ONE;
		} else {
			return first;
		}
	}

	public long queryForLong(String statement) throws SQLException {
		Object result = queryForOne(statement, new Object[0], new FieldType[0], longWrapper);
		if (result == null) {
			throw new SQLException("No results returned in query-for-long: " + statement);
		} else if (result == MORE_THAN_ONE) {
			throw new SQLException("More than 1 result returned in query-for-long: " + statement);
		} else {
			return (Long) result;
		}
	}

	public void close() throws SQLException {
		connection.close();
	}

	public boolean isClosed() throws SQLException {
		return connection.isClosed();
	}

	public boolean isTableExists(String tableName) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet results = null;
		try {
			results = metaData.getTables(null, null, "%", new String[] { "TABLE" });
			// we do it this way because some result sets don't like us to findColumn if no results
			if (!results.next()) {
				return false;
			}
			int col = results.findColumn(JDBC_META_TABLE_NAME_COLUMN);
			do {
				String dbTableName = results.getString(col);
				if (tableName.equalsIgnoreCase(dbTableName)) {
					return true;
				}
			} while (results.next());
			return false;
		} finally {
			if (results != null) {
				results.close();
			}
		}
	}

	/**
	 * Return the id associated with the column.
	 */
	private Number getIdColumnData(ResultSet resultSet, ResultSetMetaData metaData, int columnIndex)
			throws SQLException {
		int typeVal = metaData.getColumnType(columnIndex);
		switch (typeVal) {
			case Types.BIGINT :
			case Types.DECIMAL :
			case Types.NUMERIC :
				return (Number) resultSet.getLong(columnIndex);
			case Types.INTEGER :
				return (Number) resultSet.getInt(columnIndex);
			default :
				throw new SQLException("Unknown DataType for typeVal " + typeVal + " in column " + columnIndex);
		}
	}

	private void statementSetArgs(PreparedStatement stmt, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			int typeVal = H2CompiledStatement.sqlTypeToJdbcInt(argFieldTypes[i].getSqlType());
			if (arg == null) {
				stmt.setNull(i + 1, typeVal);
			} else {
				stmt.setObject(i + 1, arg, typeVal);
			}
		}
	}

	private static class OneLongWrapper implements GenericRowMapper<Long> {
		public Long mapRow(DatabaseResults rs) throws SQLException {
			// maps the first column (sql #1)
			return rs.getLong(0);
		}
	}
}
