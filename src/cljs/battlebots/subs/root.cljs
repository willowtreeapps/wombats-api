(ns battlebots.subs.root
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [battlebots.subs.user]
            [battlebots.subs.game]
            [battlebots.subs.routing]
            [battlebots.subs.sample]))

(re-frame/register-sub
  :auth-token
  (fn [db _]
    (reaction (:auth-token @db))))
