(ns wombats.views
  (:require [re-frame.core :as re-frame]
            [wombats.panels.home :as home]
            [wombats.panels.about :as about]
            [wombats.panels.admin :as admin]
            [wombats.panels.my-settings :as my-settings]
            [wombats.components.navbar :as navbar]
            [wombats.components.ui :as ui]))

(defmulti panels identity)
(defmethod panels :home-panel [] [home/home-panel])
(defmethod panels :about-panel [] [about/about-panel])
(defmethod panels :my-settings-panel [] [my-settings/my-settings-panel])
(defmethod panels :admin-panel [] [admin/admin-panel])
(defmethod panels :default [] [:div])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        active-modal (re-frame/subscribe [:active-modal])
        active-alert (re-frame/subscribe [:active-alert])]
    (fn []
      [:div
       [navbar/root]
       [ui/render-modal @active-modal]
       [ui/render-alert @active-alert]
       [:div.main-container
        [show-panel @active-panel]]])))
