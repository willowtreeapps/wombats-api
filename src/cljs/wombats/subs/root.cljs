(ns wombats.subs.root
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [wombats.subs.user]
            [wombats.subs.game]
            [wombats.subs.routing]
            [wombats.subs.ui]))

(re-frame/register-sub
  :auth-token
  (fn [db _]
    (reaction (:auth-token @db))))
