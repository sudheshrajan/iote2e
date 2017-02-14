package com.pzybrick.iote2e.tests.ignitemulti;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.avro.util.Utf8;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pzybrick.iote2e.common.utils.Iote2eUtils;
import com.pzybrick.iote2e.schema.avro.Iote2eResult;
import com.pzybrick.iote2e.schema.avro.OPERATION;
import com.pzybrick.iote2e.schema.util.Iote2eResultReuseItem;
import com.pzybrick.iote2e.schema.util.Iote2eSchemaConstants;

public class EvalIgniteMultiClient {
	private static final Logger logger = LogManager.getLogger(EvalIgniteMultiClient.class);
	private List<EvalIgniteSubscribeThread> evalIgniteSubscribeThreads;
	private List<Iote2eResultPollerThread> iote2eResultPollerThreads;
	private IgniteCache<String, byte[]> cache = null;
	private Ignite ignite = null;
	private Iote2eResultReuseItem iote2eRequestResultItem = new Iote2eResultReuseItem();	


	public static void main(String[] args) {
		try {
			EvalIgniteMultiClient evalIgniteMultiClient = new EvalIgniteMultiClient();
			evalIgniteMultiClient.process();
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void process() throws Exception {
		final int max_conns = 10;
		initIgniteSender();
		// Start listener threads, each with a different key
		evalIgniteSubscribeThreads = new ArrayList<EvalIgniteSubscribeThread>();
		iote2eResultPollerThreads = new ArrayList<Iote2eResultPollerThread>();
		
		for( int i=0 ; i<max_conns ; i++ ) {
			String igniteFilterKey = String.format("login-%d|source-%d|sensor-%d|", i, i, i );
			ConcurrentLinkedQueue<Iote2eResult> queueIote2eResults = new ConcurrentLinkedQueue<Iote2eResult>();
			Iote2eResultPollerThread iote2eResultPollerThread = new Iote2eResultPollerThread(i, queueIote2eResults);
			iote2eResultPollerThreads.add(iote2eResultPollerThread);
			iote2eResultPollerThread.start();
			EvalIgniteSubscribeThread evalIgniteSubscribeThread = EvalIgniteSubscribeThread.startThreadSubscribe( igniteFilterKey,
				queueIote2eResults, iote2eResultPollerThread );
			evalIgniteSubscribeThreads.add(evalIgniteSubscribeThread);
		}
		
		// Pump messages into ignite with different keys
		try {
			Thread.sleep(1000);
		} catch( InterruptedException e) {}
		
		for( int testNumber = 0 ; testNumber<1000 ; testNumber++) {
			for( int threadOffset=0 ; threadOffset<max_conns ; threadOffset++ ) {
				Iote2eResult iote2eResult = createIote2eResult( threadOffset, testNumber );
				String igniteFilterKey = String.format("login-%d|source-%d|sensor-%d|actuator-%d|", threadOffset, threadOffset, threadOffset, threadOffset );
				cache.put(igniteFilterKey, iote2eRequestResultItem.toByteArray(iote2eResult));
			}
//			try {
//				Thread.sleep(1000);
//			} catch( InterruptedException e) {}
		}
		
		logger.info("Sleeping 3s after all Puts");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
		
		// Shutdown listeners
		for( int i=0 ; i<max_conns ; i++ ) {
			EvalIgniteSubscribeThread evalIgniteSubscribeThread = evalIgniteSubscribeThreads.get(i);
			Iote2eResultPollerThread iote2eResultPollerThread = iote2eResultPollerThreads.get(i);
			iote2eResultPollerThread.shutdown();
			iote2eResultPollerThread.join(5000);
			evalIgniteSubscribeThread.shutdown();
			evalIgniteSubscribeThread.join(5000);
		}
		cache.close();
		ignite.close();			
	}
	
	private Iote2eResult createIote2eResult( int threadOffset, int testNumber ) {
		Map<CharSequence,CharSequence> pairs = new HashMap<CharSequence,CharSequence>();
		pairs.put( new Utf8(Iote2eSchemaConstants.PAIRNAME_SENSOR_NAME), new Utf8("sensor-"+threadOffset ));
		pairs.put( new Utf8(Iote2eSchemaConstants.PAIRNAME_ACTUATOR_NAME), new Utf8("actuator-"+threadOffset ));
		pairs.put( new Utf8(Iote2eSchemaConstants.PAIRNAME_ACTUATOR_VALUE), new Utf8(threadOffset+"-"+testNumber ));
		pairs.put( new Utf8(Iote2eSchemaConstants.PAIRNAME_ACTUATOR_VALUE_UPDATED_AT), new Utf8(Iote2eUtils.getDateNowUtc8601()));
		
		Iote2eResult iote2eResult = Iote2eResult.newBuilder()
			.setPairs(pairs)
			.setLoginName( new Utf8("login-"+threadOffset))
			.setSourceName(new Utf8("source-"+threadOffset))
			.setSourceType(new Utf8("testSourceType"))
			.setRequestUuid(new Utf8(UUID.randomUUID().toString()))
			.setRequestTimestamp(new Utf8(Iote2eUtils.getDateNowUtc8601()))
			.setOperation(OPERATION.ACTUATOR_VALUES)
			.setResultCode(0)
			.setResultTimestamp( new Utf8(Iote2eUtils.getDateNowUtc8601()))
			.setResultUuid(  new Utf8(UUID.randomUUID().toString()))
			.build();
		return iote2eResult;
	}
	
	
	private void initIgniteSender() throws Exception {
		try {
			String cacheName = "iote2e-evalmulticlient-cache";
			String igniteConfigPath = "/home/pete/development/gitrepo/iote2e/iote2e-tests/iote2e-shared/config_ignite/";
			if( igniteConfigPath == null ) throw new Exception("Required MasterConfig value igniteConfigPath is not set, try setting to location of ignite-iote2e.xml");
			if( !igniteConfigPath.endsWith("/") ) igniteConfigPath = igniteConfigPath + "/";
			String igniteConfigPathNameExt = igniteConfigPath + "ignite-iote2e-local-peer-false.xml";
			String configName = "ignite.cfg";
			logger.info("Initializing Ignite, config file=" + igniteConfigPathNameExt + ", config name=" +  configName);
			IgniteConfiguration igniteConfiguration = Ignition.loadSpringBean(
					igniteConfigPathNameExt, configName);
			Ignition.setClientMode(true);
			ignite = Ignition.getOrStart(igniteConfiguration);			
			cache = ignite.getOrCreateCache(cacheName);
		} catch(Exception e ) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	
	public class Iote2eResultPollerThread extends Thread {
		private boolean shutdown = false;
		private int offset;
		private ConcurrentLinkedQueue<Iote2eResult> queueIote2eResults;
		
		public Iote2eResultPollerThread(int offset, ConcurrentLinkedQueue<Iote2eResult> queueIote2eResults) {
			super();
			this.offset = offset;
			this.queueIote2eResults = queueIote2eResults;
		}
		
		@Override
		public void run() {
			try {
				int cntRead = 0;
				int cntError = 0;
				final String suffix = "-" + offset;
				while( true ) {
					while( !queueIote2eResults.isEmpty() ) {
						Iote2eResult iote2eResult = queueIote2eResults.poll();
						if( iote2eResult != null ) {
							cntRead++;
							if( !iote2eResult.getLoginName().toString().endsWith(suffix)) {
								cntError++;
								logger.info(">>>> ERROR Iote2eResultPollerThread {}: {}",  offset, iote2eResult);
							}
							
						}
					}
					try {
						sleep(5000);
					} catch( InterruptedException e) {}
					if( shutdown ) {
						logger.info("Shutting down Iote2eResultPollerThread {}, cntRead={}, cntError={}", offset, cntRead, cntError );
						return;
					}
				}
				
			} catch( Exception e ) {
				logger.error(e.getMessage(), e );
				return;
			}
		}
		
		public void shutdown() {
			shutdown = true;
			interrupt();
		}
		
	}

}
