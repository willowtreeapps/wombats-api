## Clojure

This is example Clojure code you can submit that results
in a Wombat that never moves.

```clojure
(fn wombat
  "the bot takes in two arguments, and returns an action"
  [state time-left]
  nil)
```

In order to generate the `.jar` file for this lamda function, you must
execute `lein uberjar` from within the `clojure` folder.

Then you must upload `/clojure/target/wombats-lambda-clojure-VERSION-standalone.jar`
to AWS Lambda.
