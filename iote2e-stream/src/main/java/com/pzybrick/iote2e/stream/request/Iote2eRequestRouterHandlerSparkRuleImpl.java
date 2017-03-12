package com.pzybrick.iote2e.stream.request;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pzybrick.iote2e.common.config.MasterConfig;
import com.pzybrick.iote2e.schema.avro.Iote2eRequest;
import com.pzybrick.iote2e.stream.svc.RuleEvalResult;
import com.pzybrick.iote2e.stream.svc.RuleSvc;

public class Iote2eRequestRouterHandlerSparkRuleImpl implements Iote2eRequestRouterHandler {
	private static final Logger logger = LogManager.getLogger(Iote2eRequestRouterHandlerSparkRuleImpl.class);
	private RuleSvc ruleSvc;
	private Iote2eSvc iote2eSvc;


	public Iote2eRequestRouterHandlerSparkRuleImpl( ) throws Exception {

	}
	
	
	public void init(MasterConfig masterConfig) throws Exception {
		try {
			Class cls = Class.forName(masterConfig.getRuleSvcClassName());
			ruleSvc = (RuleSvc) cls.newInstance();
			cls = Class.forName(masterConfig.getRequestSvcClassName());
			iote2eSvc = (Iote2eSvc) cls.newInstance();
			
			ruleSvc.init(masterConfig);
			iote2eSvc.init(masterConfig);
		} catch( Exception e ) {
			logger.error(e.getMessage(),e);
			throw e;
		}
	}
	
	public void processRequests( List<Iote2eRequest> iote2eRequests ) throws Exception {
		try {
			for( Iote2eRequest iote2eRequest : iote2eRequests ) {
				if (iote2eRequest != null) {
					List<RuleEvalResult> ruleEvalResults = ruleSvc.process( iote2eRequest);
					if (ruleEvalResults != null && ruleEvalResults.size() > 0 ) {
						iote2eSvc.processRuleEvalResults( iote2eRequest, ruleEvalResults);
					}
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw e;
		}
	}

}