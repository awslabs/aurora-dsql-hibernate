## My Project

# Aurora DSQL dialect for Hibernate

This repository contains the associated examples and dialect code for working with Aurora DSQL.

## Requirements

- You must have an AWS account, and have your default credentials and AWS Region
  configured as described in the
  [Globally configuring AWS SDKs and tools](https://docs.aws.amazon.com/credref/latest/refdocs/creds-config-files.html)
  guide.
- Java Development Kit (JDK): Ensure you have JDK 17+ installed.

  _To verify the java is installed, you can run_

  ```bash
  java -version
  ```

- Build Tool (Maven or Gradle)
  - _Maven_: Ensure Maven is installed if that is your preferred option. You can download it from the [official website](https://maven.apache.org/download.cgi).
  - _Gradle_: Ensure Gradle is installed if that is your preferred option. You can download it from the [official website](https://gradle.org/install/).
- AWS SDK: Ensure that you setup the latest version of the AWS Java SDK [official website](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup.html)
- You must have an Aurora DSQL cluster. For information about creating an Aurora DSQL cluster, see the
  [Getting started with Aurora DSQL](https://docs.aws.amazon.com/aurora-dsql/latest/userguide/getting-started.html)
  guide.
- If connecting as a non-admin user, ensure the user is linked to an IAM role and is granted access to the `myschema`
  schema. See the
  [Using database roles with IAM roles](https://docs.aws.amazon.com/aurora-dsql/latest/userguide/using-database-and-iam-roles.html)
  guide.

## ⚠️ Important

- Running this code might result in charges to your AWS account.
- We recommend that you grant your code least privilege. At most, grant only the
  minimum permissions required to perform the task. For more information, see
  [Grant least privilege](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html#grant-least-privilege).
- This code is not tested in every AWS Region. For more information, see
  [AWS Regional Services](https://aws.amazon.com/about-aws/global-infrastructure/regional-product-services).

## Getting Started

To run included example please refer to [README](https://github.com/awslabs/aurora-dsql-hibernate/blob/main/examples/pet-clinic-app/README.md)

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1
