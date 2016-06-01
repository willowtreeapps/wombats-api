(ns battlebots.subs.ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :active-modal
 (fn [db _]
   (reaction (:active-modal @db))))

(re-frame/register-sub
 :active-alert
 (fn [db _]
   (reaction (:active-alert @db))))
