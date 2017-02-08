(ns user
  (:require [reloaded.repl :refer [system init start stop go reset]]
            [com.stuartsierra.component :as component]
            [wombats.system :as my-system]))

(reloaded.repl/set-init! #(my-system/system))
