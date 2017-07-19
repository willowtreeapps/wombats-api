#!/bin/bash

[ -z "$AWS_ACCESS_KEY_ID" ] && { echo "You must provide environment variable AWS_ACCESS_KEY_ID"; exit 1; }
[ -z "$AWS_SECRET_ACCESS_KEY" ] && { echo "You must provide environment variable AWS_SECRET_ACCESS_KEY"; exit 1; }

mkdir -p lambda/bundle
zip lambda/bundle/python.zip lambda/python/handler.py lib/wombat_lib.py
aws lambda update-function-code \
  # Providing just the function name should work, but if not could be replaced by full ARN
  --function-name womabts-python --zip-file fileb://lambda-bundle/python.zip


zip lambda/bundle/javascript.zip lambda/javascript/index.js lib/wombat_lib.js
aws lambda update-function-code \
  --function-name wombats-javacript --zip-file fileb://lambda-bundle/javascript.zip

# Is there a better way to add the clojure library to the standard path?
ln -f lib/wombat_lib.clj lambda/clojure/src/wombat_lib.clj
cd lambda/clojure
lein uberjar
aws lambda update-function-code \
  --function-name wombats-clojure --zip-file target/wombats-lambda-clojure-0.1.0-standalone.jar
rm -r target
cd ..
