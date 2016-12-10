package com.pzybrick.test.iote2e.ruleproc.kafka;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.pzybrick.iote2e.ruleproc.svc.RuleEvalResult;

import junit.framework.Assert;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestKafkaHandlerHumidityToMister extends TestKafkaHandlerBase {
	private static final Log log = LogFactory.getLog(TestKafkaHandlerHumidityToMister.class);
	private static String testLoginName = "lo1";
	private static String testSourceName = "lo1so1";
	private static String testSourceType = "humidity";
	private static String testSensorName = "humidity1";
	
	public TestKafkaHandlerHumidityToMister() {
		super();
	}
	
	@Test
	public void testHumidityToMisterRuleFireFanOff() throws Exception {
		log.info("begins");

		String testValue = "50";
		commonRun( testLoginName, testSourceName, testSourceType, testSensorName, testValue);
		List<RuleEvalResult> ruleEvalResults = commonGetRuleEvalResults( 2000 );
		Assert.assertNotNull("ruleEvalResults must not be null", ruleEvalResults );
		Assert.assertEquals("ruleEvalResults must have size=1", 1, ruleEvalResults.size() );
		Assert.assertEquals("ruleEvalResults getActuatorTargetValue", "on", ruleEvalResults.get(0).getActuatorTargetValue() );
	}
	
	@Test
	public void testHumidityToMisterRuleFireFanOn() throws Exception {
		log.info("begins");
		String testValue = "100";
		commonRun( testLoginName, testSourceName, testSourceType, testSensorName, testValue);
		List<RuleEvalResult> ruleEvalResults = commonGetRuleEvalResults( 2000 );
		Assert.assertNotNull("ruleEvalResults must not be null", ruleEvalResults );
		Assert.assertEquals("ruleEvalResults must have size=1", ruleEvalResults.size(), 1 );
		Assert.assertEquals("ruleEvalResults getActuatorTargetValue", "off", ruleEvalResults.get(0).getActuatorTargetValue() );
	}
	
	@Test
	public void testHumidityToMisterRuleNotFire() throws Exception {
		log.info("begins");
		String testValue = "87";
		commonRun( testLoginName, testSourceName, testSourceType, testSensorName, testValue);
		List<RuleEvalResult> ruleEvalResults = commonGetRuleEvalResults( 2000 );
		Assert.assertNull("ruleEvalResults must be null", ruleEvalResults );
	}
}