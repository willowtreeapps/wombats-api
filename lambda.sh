#!/bin/bash

[ -z "$1" ] && { echo "You must provide the lambda function prefix as the first argument\ne.g. ./lambda.sh wombats-prod"; exit 1; }
[ -z "$AWS_ACCESS_KEY_ID" ] && { echo "You must provide environment variable AWS_ACCESS_KEY_ID"; exit 1; }
[ -z "$AWS_SECRET_ACCESS_KEY" ] && { echo "You must provide environment variable AWS_SECRET_ACCESS_KEY"; exit 1; }

mkdir -p lambda/bundle
zip lambda/bundle/$1-python.zip lambda/python/handler.py src/wombats/lib/wombat_lib.py
echo -e "Updating function $1-python\n"
aws lambda update-function-code \
  # Providing just the function name should work, but if not could be replaced by full ARN
  --function-name $1-python --zip-file fileb://lambdar/bundle/$1-python.zip

zip lambda/bundle/$1-javascript.zip lambda/javascript/index.js src/wombats/lib/wombat_lib.js
echo -e "Updating Lambda function $1-javascript\n"
aws lambda update-function-code \
  --function-name $1-javascript --zip-file fileb://lambda/bundle/$1-javascript.zip

# Is there a better way to add the clojure library to the standard path?
ln -f lib/wombat_lib.clj lambda/clojure/src/wombat_lib.clj
cd lambda/clojure
lein uberjar
echo -e "Updating Lambda function $1-clojure\n"
aws lambda update-function-code \
  --function-name $1-clojure --zip-file target/wombats-lambda-clojure-0.1.0-standalone.jar
rm -r target
cd ..
