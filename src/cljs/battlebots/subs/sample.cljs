(ns battlebots.subs.sample
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
  :word
  (fn [db _]
    (reaction (:word @db))))
