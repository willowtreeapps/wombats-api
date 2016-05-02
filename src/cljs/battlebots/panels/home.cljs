(ns battlebots.panels.home
  (:require [re-frame.core :as re-frame]))

(defn home-panel []
  (fn []
    [:div.panel-home
     [:h1 "Home"]]))
