(ns battlebots.panels.admin
  (:require [re-frame.core :as re-frame]
            [reagent.core :refer [atom]]
            [battlebots.components.sortable-table :refer [sortable-table]]))

(def active-table (atom {:name nil}))

(defn is-active?
  "determins if a given table is active"
  [table-name]
  (= table-name (:name @active-table)))

(defn set-active
  "sets active table"
  [table-name]
  (swap! active-table assoc :name table-name))

;;
;; Table configurations
;;
(defn render-users
  "renders users table"
  []
  (let [users (re-frame/subscribe [:users])]
    (sortable-table {:class-name "user-panel"
                     :collection @users
                     :record-id-key :_id
                     :aliases {:_id "User ID"
                               :username "Username"
                               :bot-repo "Repository"
                               :roles "User Roles"
                               :remove "Remove User"}
                     :formatters {:remove (fn [record]
                                            [:button {:on-click #(re-frame/dispatch [:remove-user (:_id record)])} "Remove User"])}})))

(defn render-games
  "renders games table"
  []
  (let [games (re-frame/subscribe [:games])]
    [:div
     [:input.btn {:type "button"
                  :value "Add Game"
                  :on-click #(re-frame/dispatch [:create-game])}]
     (sortable-table {:class-name "game-panel"
                      :collection (map #(dissoc % :initial-arena) @games)
                      :record-id-key :_id
                      :aliases {:_id "Game ID"}
                      :formatters {:remove (fn [record]
                                             [:button {:on-click #(re-frame/dispatch [:remove-game (:_id record)])} "Remove Game"])}})]))

(defn show-active-panel
  []
  (cond
   (is-active? :users) (render-users)
   (is-active? :games) (render-games)))

(defn admin-panel []
  (re-frame/dispatch [:fetch-users])
  (re-frame/dispatch [:fetch-games])
  (fn []
    [:div.panel-admin
     [:h1 "Admin Panel"]
     [:button {:on-click #(set-active :users)} "Users"]
     [:button {:on-click #(set-active :games)} "Games"]
     (show-active-panel)]))
