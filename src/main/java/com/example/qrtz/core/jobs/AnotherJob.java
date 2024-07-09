package com.example.qrtz.core.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnotherJob implements Job {
	private static final Logger logger = LoggerFactory.getLogger(AnotherJob.class);
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.info("Another Job: " + context.getJobDetail().getKey());
	}

}
