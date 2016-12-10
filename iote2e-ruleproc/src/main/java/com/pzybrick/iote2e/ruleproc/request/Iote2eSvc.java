package com.pzybrick.iote2e.ruleproc.request;

import java.util.List;

import com.pzybrick.iote2e.ruleproc.svc.RuleConfig;
import com.pzybrick.iote2e.ruleproc.svc.RuleEvalResult;
import com.pzybrick.iote2e.schema.avro.Iote2eRequest;

public interface Iote2eSvc {
	public abstract void init(RuleConfig ruleConfig) throws Exception;
	public void close() throws Exception;
	public void processRuleEvalResults( Iote2eRequest iote2eRequest, List<RuleEvalResult> ruleEvalResults)
			throws Exception;
}