(ns battlebots.subs.user
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
  :user
  (fn [db _]
    (reaction (:user @db))))
