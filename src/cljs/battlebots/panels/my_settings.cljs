(ns battlebots.panels.my-settings
  (:require [re-frame.core :as re-frame]))

(defn my-settings-panel []
  (let [user (re-frame/subscribe [:user])]
    (println @user)
    (fn []
      [:div.panel-my-settings
       [:h2 (:login @user)]
       [:img {:src (:avatar_url @user)
              :alt (str (:login @user) "'s avatar")
              :class-name "avatar"}]
       [:p "Settings"]])))
