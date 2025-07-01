# DSQL Integration using JDBC with HikariCP and Hibernate Guide

## Overview

This code example demonstrates how to use Hibernate (with pgJDBC and Hikari connection pool)
with Amazon Aurora SQL (DSQL). The example shows you how to connect Hibernate to an Aurora DSQL cluster,
performs a scheduled refresh of the DSQL token based password and perform basic database operations by leveraging the
SpringPetClinic sample logic.

Aurora DSQL is a distributed SQL database service that provides high availability and scalability for
your PostgreSQL-compatible applications.

- `pgJDBC` is the official PostgreSQL adapter for Java that allows you to interact with PostgreSQL databases using Java code.

- `HikariCP` is a popular Java connection pool that allows you to manage a pool of jdbc connection using Java code.

- `Hibernate` is the most popular Object-Relational mapping frameowrk for Java that allows you to interact a variety of databases using Java code.

## ⚠️ Important

- Running this code might result in charges to your AWS account.
- We recommend that you grant your code least privilege. At most, grant only the
  minimum permissions required to perform the task. For more information, see
  [Grant least privilege](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html#grant-least-privilege).
- This code is not tested in every AWS Region. For more information, see
  [AWS Regional Services](https://aws.amazon.com/about-aws/global-infrastructure/regional-product-services).

## About the code example

The example demonstrates a flexible connection approach that works for both admin and non-admin users:

- When connecting as an **admin user**, the example uses the `public` schema and generates an admin authentication
  token.
- When connecting as a **non-admin user**, the example uses a custom `myschema` schema and generates a standard
  authentication token.

The code automatically detects the user type and adjusts its behavior accordingly.

## Prerequisites

- You must have an AWS account, and have your default credentials and AWS Region
  configured as described in the
  [Globally configuring AWS SDKs and tools](https://docs.aws.amazon.com/credref/latest/refdocs/creds-config-files.html)
  guide.
- If connecting as a non-admin user, ensure the user is linked to an IAM role and is granted access to the `myschema`
  schema. See the
  [Using database roles with IAM roles](https://docs.aws.amazon.com/aurora-dsql/latest/userguide/using-database-and-iam-roles.html)
  guide.
- Verify that your version of Java is 1.8 or higher
- AWS SDK for Java
- DDL population scripts depend on `aws` cli and `psql` client to be installed.
  - <https://docs.aws.amazon.com/cli/>
  - <https://docs.aws.amazon.com/aurora-dsql/latest/userguide/getting-started.html#accessing-sql-clients-psql>
- Alternatively use <https://docs.aws.amazon.com/aurora-dsql/latest/userguide/what-is-aurora-dsql.html>

## Run the example

# Cloud Shell DDL Setup (Optional)

1. Your IAM identity must have permission to [sign in AWS Management Console](https://docs.aws.amazon.com/signin/latest/userguide/console-sign-in-tutorials.html)
2. Sign in to the AWS Management Console and open the Aurora DSQL console at <https://console.aws.amazon.com/dsql>
3. Navigate to your cluster: [Clusters](https://us-east-1.console.aws.amazon.com/dsql/clusters/home)
4. Select your cluster (eg. h4abtt2drceddyfw4ylkrmv2nm)
5. Open CloudShell
6. Using Actions menu | Upload file | create_petclinic.sh
7. Using Actions menu | Upload file | petclinic.sql
8. Cluster Actions menu | Connect
   - Copy Endpoint (Host)
9. CloudShell Terminal

   ```bash
     export CLUSTER_ENDPOINT=<Paste endpoint>
     export REGION=<Cluster region>
     export CLUSTER_USER=admin
     export CLUSTER_SCHEMA=postgres
   ```

# Linux/Mac Gradle

```bash
# create_petclinic.sh step is optional if completed in Cloud Shell
./create_petclinic.sh
./gradlew clean
./gradlew bootRun
```

# Linux/Mac Maven

```bash
# create_petclinic.sh step is optional if completed in Cloud Shell
./create_petclinic.sh
./mvnw clean
./mvnw spring-boot:run
```

# Windows

```bash
# create_petclinic.bat step is optional if completed in Cloud Shell
create_petclinic.bat
gradlew.bat clean
gradlew.bat bootRun
```

# DSQL code examples

## Setting Up Connection Properties and Generating DSQL Token

To connect to the DSQL server, configure the username, URL endpoint, and password by setting the properties in `HikariDataSource` as shown below:

```java
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

  @Value("${app.dsql.username:admin}")
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

    // set the password by generating token from credentials.
    generateToken();
    return hds;
  }

 @Scheduled(fixedRateString = "${app.dsql.token.refresh-interval:600000}")
  public void generateToken() {
    // Generate and set the DSQL token
    final DsqlUtilities utilities = DsqlUtilities.builder()
            .region(Region.of(region.toLowerCase()))
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build();

    final Consumer<GenerateAuthTokenRequest.Builder> requester =
            builder -> builder.hostname(dataSource.getJdbcUrl().split("/")[2])
                    .region(Region.of(region))
                    .expiresIn(Duration.ofMillis(maxLifetime)); // Default is 900 seconds

    // Use auth method according to the current user. The admin user is assumed to be "admin".
    final String token = username.equals("admin")
            ? utilities.generateDbConnectAdminAuthToken(requester)
            : utilities.generateDbConnectAuthToken(requester);


    dataSource.setPassword(token);
    logger.info("Generated DSQL token");
  }
}

```

The token is generated using AWS credentials and is set to expire after a certain duration.
This sample handles token expiration and regeneration in application logic to maintain a valid connection to the DSQL server on a scheduled interval.

## Using UUID as Primary Key

DSQL does not support serialized primary keys or identity columns (auto-incrementing integers) that are commonly used in traditional relational databases. Instead, it is recommended to use UUID (Universally Unique Identifier) as the primary key for your entities.

Here's how to define a UUID primary key in your entity class:

```java
@Id
@Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
private UUID id;
```

When using this approach, you don't need to manually set the ID when creating new entities. The database will automatically generate a new UUID for each inserted row.

Remember to import the necessary UUID class:

```java
import java.util.UUID;
```

## Defining Entity Classes

Hibernate can automatically create/validate database tables based on your entity class definitions. Here's a simple example of how to define an entity class:

```java
import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import org.hibernate.annotations.Generated;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class Person implements Serializable {

    @GeneratedValue
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @Column(name = "first_name")
    @NotBlank
    private String firstName;

    // Getters and setters
}
```

## Handling SQL Exceptions

To handle specific SQL exceptions (like 0C001, 0C000, 0A000) without evicting connections, implement a custom SQLExceptionOverride:

```java

public class DsqlExceptionOverride implements SQLExceptionOverride {
    @Override
    public Override adjudicate(SQLException ex) {
        final String sqlState = ex.getSQLState();

        if ("0C000".equalsIgnoreCase(sqlState) || "0C001".equalsIgnoreCase(sqlState) || (sqlState).matches("0A\\d{3}")) {
            return SQLExceptionOverride.Override.DO_NOT_EVICT;
        }

        return Override.CONTINUE_EVICT;
    }
}
```

Then, set this class in your HikariCP configuration:

```java
@Configuration(proxyBeanMethods = false)
public class DsqlDataSourceConfig {

    @Bean
    public HikariDataSource dataSource() {
        final DataSourceProperties properties = new DataSourceProperties();
        // ... other properties

        final HikariDataSource hds = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();

        // handle the connection eviction for known exception types.
        hds.setExceptionOverrideClassName(DsqlExceptionOverride.class.getName());

        // ... other properties

        return hds;
    }
}
```

## Relationships

For `@OneToMany` and `@ManyToMany` relationships, DSQL works similarly to standard Hibernate implementations. These relationships can be used to model associations between entities in your database.

For detailed information on how to use these relationships with Hibernate, please refer to the official Hibernate documentation:

- [Hibernate ORM 6.2 User Guide - Associations](https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#associations)

This guide provides comprehensive information on:

- One-To-Many associations
- Many-To-Many associations
- Bidirectional associations
- Association mappings
- Collection mappings

When working with DSQL, you can follow these Hibernate guidelines for setting up your entity relationships. The main difference will be in the use of UUID for primary and foreign keys(as DSQL does not support foreign keys), as discussed in the [Using UUID as Primary Key](#using-uuid-as-primary-key) section.
