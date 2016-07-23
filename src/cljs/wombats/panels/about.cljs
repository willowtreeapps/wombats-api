(ns wombats.panels.about
  (:require [re-frame.core :as re-frame]))

(defn about-panel []
  (fn []
    [:div.panel-about
     [:h1 "About"]]))
