package org.springframework.samples.petclinic.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;

import java.util.function.Consumer;
import java.time.Duration;
import java.util.logging.Logger;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class DsqlDataSourceConfig {

	final Logger logger = Logger.getLogger(this.toString());

	@Value("${app.dsql.action:DB_CONNECT}")
	private String action;

	@Value("${app.dsql.region:US_EAST_1}")
	private String region;

	@Value("${app.dsql.token.refresh-rate:1800000}")
	private long maxLifetime;

	@Value("${app.datasource.username:admin}")
	private String username;

	private HikariDataSource dataSource;

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource")
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	public HikariDataSource dataSource(DataSourceProperties properties) {
		final HikariDataSource hds = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
		hds.setMaxLifetime(maxLifetime);
		this.dataSource = hds;
		hds.setExceptionOverrideClassName(DsqlExceptionOverride.class.getName());

		// Set the schema based on user type
		if (!username.equals("admin")) {
			hds.addDataSourceProperty("currentSchema", "myschema");
			logger.info("Set schema to myschema");
		}

		// set the password by generating token from credentials.
		generateToken();
		return hds;
	}

	@Scheduled(fixedRateString = "${app.dsql.token.refresh-interval:600000}")
	public void generateToken() {
		// Generate and set the DSQL token
		logger.info("Region: " + region);
		final DsqlUtilities utilities = DsqlUtilities.builder()
			.region(Region.of(region.toLowerCase()))
			.credentialsProvider(DefaultCredentialsProvider.create())
			.build();

		logger.info("Generating DSQL token for user:" + username);

		final Consumer<GenerateAuthTokenRequest.Builder> requester = builder -> builder
			.hostname(dataSource.getJdbcUrl().split("/")[2])
			.region(Region.of(region))
			.expiresIn(Duration.ofMillis(maxLifetime)); // Default is 900 seconds

		// Use auth method according to the current user. The admin user is assumed to be
		// "admin".
		final String token = username.equals("admin") ? utilities.generateDbConnectAdminAuthToken(requester)
				: utilities.generateDbConnectAuthToken(requester);

		logger.info("Using this token: " + token);

		dataSource.setPassword(token);
		logger.info("Generated DSQL token");
	}

}
