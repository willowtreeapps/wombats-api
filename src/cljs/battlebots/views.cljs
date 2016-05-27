(ns battlebots.views
  (:require [re-frame.core :as re-frame]
            [battlebots.panels.home :as home]
            [battlebots.panels.about :as about]
            [battlebots.panels.admin :as admin]
            [battlebots.panels.signin :as signin]
            [battlebots.panels.signup :as signup]
            [battlebots.components.navbar :as navbar]))

(defmulti panels identity)
(defmethod panels :home-panel [] [home/home-panel])
(defmethod panels :about-panel [] [about/about-panel])
(defmethod panels :admin-panel [] [admin/admin-panel])
(defmethod panels :signin-panel [] [signin/signin-panel])
(defmethod panels :signup-panel [] [signup/signup-panel])
(defmethod panels :default [] [:div])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div
        [navbar/root]
        [:div.main-container
         [show-panel @active-panel]]])))
