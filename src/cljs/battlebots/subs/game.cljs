(ns battlebots.subs.game
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :games
 (fn [db _]
   (reaction (:games @db))))

(re-frame/register-sub
 :active-game
 (fn [db _]
   (reaction (:active-game @db))))

(re-frame/register-sub
 :active-frame
 (fn [db _]
   (reaction (:active-frame @db))))
