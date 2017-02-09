package com.pzybrick.iote2e.ruleproc.persist;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.pzybrick.iote2e.common.persist.CassandraBaseDao;
import com.pzybrick.iote2e.ruleproc.svc.ActuatorState;

public class ActuatorStateDao extends CassandraBaseDao {
	private static final Logger logger = LogManager.getLogger(ActuatorStateDao.class);
	private static final String TABLE_NAME = "actuator_state";
	private static final String CREATE_TABLE = 
			"CREATE TABLE " + TABLE_NAME + "( " + 
			"	login_source_sensor text PRIMARY KEY, " + 
			"	actuator_name text, " + 
			"	actuator_value text, " + 
			"	actuator_desc text, " + 
			"	actuator_value_updated_at timestamp " + 
			");";

	
	public static void createTable( ) throws Exception {
		try {
			logger.debug("createTable={}",CREATE_TABLE);
			execute(CREATE_TABLE);

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	public static void dropTable( ) throws Exception {
		try {
			logger.debug("dropTable={}",TABLE_NAME);
			execute("DROP TABLE IF EXISTS " + TABLE_NAME + "; ");

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	
	public static void updateActuatorValue( String pk, String newValue ) throws Exception {
		try {
			String update = createUpdateValueCql( pk, newValue );
			logger.debug("update={}",update);
			execute(update);

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	public static String findActuatorValue( String pk ) throws Exception {
		try {
			String select = String.format("SELECT actuator_value FROM %s where login_source_sensor='%s'; ", TABLE_NAME, pk);
			logger.debug("select={}",select);
			ResultSet rs = execute(select);
			Row row = rs.one();
			return row.getString("actuator_value");

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	
	public static ActuatorState findActuatorState( String pk ) throws Exception {
		try {
			String select = String.format("SELECT * FROM %s where login_source_sensor='%s'; ", TABLE_NAME, pk);
			logger.debug("select={}",select);
			ResultSet rs = execute(select);
			Row row = rs.one();
			return actuatorStateFromRow( row );

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	
	public static long count( ) throws Exception {
		return count(TABLE_NAME);
	}
	
	public static void truncate( ) throws Exception {
		truncate(TABLE_NAME);
	}
	
	public static boolean isTableExists( String keyspaceName ) throws Exception {
		return isTableExists(keyspaceName, TABLE_NAME);
	}
	
	public static void deleteRow( String pk ) throws Exception {
		try {
			String delete = String.format("DELETE FROM %s where login_source_sensor='%s'; ", TABLE_NAME, pk);
			logger.debug("delete={}",delete);
			execute(delete);

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	public static ActuatorState actuatorStateFromRow( Row row ) {
		if( row == null ) return null;
		String loginSourceSensor = row.getString("login_source_sensor");
		if( loginSourceSensor == null ) return null;	// not found
		String[] losose = loginSourceSensor.split("[|]");
		ActuatorState actuatorState = new ActuatorState().setLoginName(losose[0]).setSourceName(losose[1])
				.setSensorName(losose[2])
				.setActuatorName(row.getString("actuator_name"))
				.setActuatorValue(row.getString("actuator_value"))
				.setActuatorDesc(row.getString("actuator_desc"))
				.setActuatorValueUpdatedAt( new DateTime(row.getTimestamp("actuator_value_updated_at")).withZone(DateTimeZone.UTC).toString() );
		return actuatorState;
	}

	
	public static void insertActuatorState( ActuatorState actuatorState) throws Exception {
		try {
			logger.debug("actuatorState={}",actuatorState.toString());
			String insert = createInsertActuatorState( actuatorState );
			logger.debug("insert={}",insert);
			execute(insert);		

		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	
	public static void insertActuatorStateBatch( List<ActuatorState> actuatorStates ) throws Exception {
		try {
			logger.debug( "inserting actuatorState {} batch rows", actuatorStates.size());
			StringBuilder sb = new StringBuilder("BEGIN BATCH\n");
			for( ActuatorState actuatorState : actuatorStates ) {
				sb.append( createInsertActuatorState( actuatorState )).append("\n");
			}
			sb.append("APPLY BATCH;");
			logger.debug("insert batch={}", sb.toString());
			execute(sb.toString());
			
		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	public static void resetActuatorStateBatch( List<ActuatorState> actuatorStates ) throws Exception {
		try {
			logger.debug( "Resetting actuatorState {} batch rows", actuatorStates.size());
			StringBuilder sb = new StringBuilder("BEGIN BATCH\n");
			for( ActuatorState actuatorState : actuatorStates ) {
				sb.append( createUpdateValueCql( actuatorState.getPk(), null )).append("\n");
			}
			sb.append("APPLY BATCH;");
			logger.debug("insert batch={}", sb.toString());
			execute(sb.toString());
			
		} catch( Exception e ) {
			logger.error(e.getLocalizedMessage(), e);
			throw e;
		}
	}
	
	
	public static String createInsertActuatorState( ActuatorState actuatorState ) {
		String key = actuatorState.getLoginName() + "|" +
				actuatorState.getSourceName() + "|" +
				actuatorState.getSensorName();
		String insActuatorValue = actuatorState.getActuatorValue() != null ? "'"+actuatorState.getActuatorValue()+"'" : "null";
		String insert = String.format("INSERT INTO %s " + 
			"(login_source_sensor,actuator_name,actuator_value,actuator_desc,actuator_value_updated_at) " + 
			"values('%s','%s',%s,'%s',toTimestamp(now())); ",
			TABLE_NAME, key, actuatorState.getActuatorName(), insActuatorValue,
			actuatorState.getActuatorDesc() );
		return insert;
	}
	
	private static String createUpdateValueCql( String pk, String newValue  ) {
		if( newValue != null ) newValue = "'" + newValue + "'";
		return String.format("UPDATE %s SET actuator_value=%s,actuator_value_updated_at=toTimestamp(now()) where login_source_sensor='%s'; ", 
				TABLE_NAME, newValue, pk);
	}
	
}
