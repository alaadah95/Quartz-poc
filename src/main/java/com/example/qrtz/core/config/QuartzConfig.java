package com.example.qrtz.core.config;

import java.util.Date;
import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import com.example.qrtz.core.jobs.AnotherJob;
import com.example.qrtz.core.jobs.HelloJob;

@Configuration
public class QuartzConfig {

	private static final Logger logger = LoggerFactory.getLogger(QuartzConfig.class);

	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private DataSource dataSource;

	@Bean
	public JobFactory jobFactory() {
		SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

	@Bean
	public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
		schedulerFactoryBean.setDataSource(dataSource);
		schedulerFactoryBean.setSchedulerName("YourSchedulerName");
		schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");
		schedulerFactoryBean.setQuartzProperties(quartzProperties());
		return schedulerFactoryBean;
	}

	private Properties quartzProperties() {
		Properties properties = new Properties();
		properties.setProperty("org.quartz.scheduler.instanceName", "ClusteredScheduler");
		properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
//	    properties.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
		properties.setProperty("org.quartz.jobStore.driverDelegateClass",
				"org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
//	    properties.setProperty("org.quartz.jobStore.useProperties", "true");
		properties.setProperty("org.quartz.jobStore.isClustered", "true");
//	    properties.setProperty("org.quartz.jobStore.clusterCheckinInterval","20000"); 
		properties.setProperty("org.quartz.jobStore.tablePrefix", "qrtz_");
		properties.setProperty("org.quartz.threadPool.threadCount", "20");
		return properties;
	}

	@Bean("jobDetail")
	public JobDetail jobDetail() {
		return JobBuilder.newJob(HelloJob.class).withIdentity(HelloJob.class.getSimpleName()).storeDurably().build();
	}

	@Bean("trigger")
	public Trigger trigger(JobDetail jobDetail) {
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
				.withIdentity(HelloJob.class.getSimpleName() + "Trigger")
				.startAt(new Date(System.currentTimeMillis() + 5000L))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();

		logger.info("Trigger created: {}", trigger.getKey());
		return trigger;

	}

	@Bean("anotherJobDetail")
	public JobDetail anotherJobDetail() {
		return JobBuilder.newJob(AnotherJob.class).withIdentity("anotherJob", "DEFAULT").storeDurably().build();
	}

	@Bean("anotherJobTrigger")
	public Trigger anotherJobTrigger() {
		return TriggerBuilder.newTrigger().forJob(anotherJobDetail()).withIdentity("anotherTrigger", "DEFAULT")
				.startNow().startAt(new Date(System.currentTimeMillis() + 5000L))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
	}

	@Bean
	public Scheduler scheduler(SchedulerFactoryBean factory, @Qualifier("jobDetail") JobDetail jobDetail,
			@Qualifier("trigger") Trigger trigger, @Qualifier("anotherJobDetail") JobDetail anotherjobDetail,
			@Qualifier("anotherJobTrigger") Trigger anothertrigger) throws SchedulerException {
		Scheduler scheduler = factory.getScheduler();
		// Check if the job already exists
		if (!scheduler.checkExists(jobDetail.getKey())) {
			scheduler.scheduleJob(jobDetail, trigger);
		} else if (!scheduler.checkExists(anotherjobDetail.getKey())) {
			scheduler.scheduleJob(anotherjobDetail, anothertrigger);
		} else {
			// Optionally log a message or handle the case where the job already exists
			System.out.println("Jobs already exist: " + jobDetail.getKey() + " & " + anotherjobDetail.getKey() );
		}
		scheduler.start();
		logger.info("Scheduler started");
		return scheduler;
	}
}
