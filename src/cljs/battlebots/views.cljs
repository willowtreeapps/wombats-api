(ns battlebots.views
  (:require [re-frame.core :as re-frame]
            [battlebots.panels.home :as home]
            [battlebots.panels.about :as about]
            [battlebots.panels.playground :as playground]
            [battlebots.components.navbar :as navbar]))

(defmulti panels identity)
(defmethod panels :home-panel [] [home/home-panel])
(defmethod panels :about-panel [] [about/about-panel])
(defmethod panels :playground-panel [] [playground/root-panel])
(defmethod panels :default [] [:div])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div
        [navbar/root active-panel]
        [:div.main-container
         [show-panel @active-panel]]])))
