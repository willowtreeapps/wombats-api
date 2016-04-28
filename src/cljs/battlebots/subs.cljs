(ns battlebots.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [register-sub]]))

(register-sub
 :word
 (fn [db]
   (reaction (:word @db))))

