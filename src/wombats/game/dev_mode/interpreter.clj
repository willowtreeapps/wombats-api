(ns wombats.game.dev-mode.interpreter
  (:require [cheshire.core :as cheshire]
            [clojure.java.io :refer [writer]]
            [clojure.java.shell :refer [sh]]))

(defn handler
  "This runs some code by creating a script and running it against an
  interpreter. It feeds it the state on STDIN and reads the next move
  from STDOUT.

  If the subprocess throws an error, that's translated into an
  exception."
  [ext interpreter state code]
  (let [tmp-file (java.io.File/createTempFile "bot" ext)]
    (try
      (with-open [file (writer tmp-file)]
        (.write file code))
      (let [proc (sh interpreter (.getAbsolutePath tmp-file)
                     :in (java.io.StringReader. state))]
        (if (zero? (:exit proc))
          (:out proc)
          (throw (Exception. (:err proc)))))
      (finally
        (.delete tmp-file)))))

