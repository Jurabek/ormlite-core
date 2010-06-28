package com.j256.ormlite.stmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.tree.RowMapper;

/**
 * Parameterized version of {@link RowMapper} so we can better genericize things.
 * 
 * @param <T>
 *            Type that the mapRow returns.
 * @author graywatson
 */
public interface GenericRowMapper<T> {

	/**
	 * Used to map a {@link ResultSet} to an object. Part of {@link ParameterizedRowMapper}.
	 * 
	 * @return The created object with all of the fields set from the {@link ResultSet}.
	 * @throws SQLException
	 *             If we could not get the SQL results or instantiate the object.
	 */
	public T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
