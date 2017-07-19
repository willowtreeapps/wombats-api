# Wombats

![Wombat](https://cloud.githubusercontent.com/assets/4649439/17083937/59e5a5f0-517d-11e6-92a2-976aee52d95c.png)

[![CircleCI](https://circleci.com/gh/willowtreeapps/wombats-api.svg?style=svg)](https://circleci.com/gh/willowtreeapps/wombats-api)

### About Wombats

TODO: Mission Statement / Goals

### Documentation

[Documentation Home](./docs/README.md)

### Development

TODO: Basic info

Want to contribute? [See how](./CONTRIBUTING.md) you can get involved.

[Getting Started Guide](./docs/development/getting-started.md)

[Development Documentation](./docs/development/README.md)

### Lambda

We use AWS Lambda to run wombat code in dev, qa, and production environments, with more parts of the API being moved to lambda soon

In order to update the code running in lambda, use the provided script `lambda.sh`

To use `lambda.sh`, you need the environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, and you must provide the prefix for the lambda functions as the first argument

##### Usage
```bash
# Replace <lambda_prefix> with the prefix of the lambda function, e.g. wombats-prod or wombats-qa
./lambda.sh <lambda_prefix>
```

### Decision Log

This is where all project decisions that affect the team will be logged.

[Decision Log](./docs/decision-logs/README.md)

### License Information

MIT License

Copyright (c) 2017 WillowTree, Inc.

### Privacy Information

TODO:
