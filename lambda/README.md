# wombats-lambda

Using AWS Lambda, we will run each bot's code every frame.

The following arguments are what the Lambda function will receive:
- Code
- State

The Lambda function will execute the passed in code, using
the `state` and `time-left` as parameters to the passed in code.

The bot's code must return an action within 3 seconds. If
it doesn't, the Lambda function will timeout and resolve
with no action.

Currently you can only submit code written in Clojure, JavaScript, or Python, however that
list will be growing soon.


## Contributing? Issues? Features? Curiosity?
[Learn how to contribute here.](https://github.com/willowtreeapps/wombats-lambda/blob/develop/CONTRIBUTING.md)
