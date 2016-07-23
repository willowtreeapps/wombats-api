(ns wombats.panels.admin
  (:require [re-frame.core :as re-frame]
            [reagent.core :refer [atom]]
            [wombats.components.sortable-table :refer [sortable-table]]))

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
(defn remove-user-action
  "determin if a user should be removed"
  [record]
  (re-frame/dispatch [:display-alert
                      {:title (str "Are you sure you want to remove " (:username record) "?")
                       :confirmed #(re-frame/dispatch [:remove-user (:_id record)])
                       :type :option}]))

(defn render-users
  "renders users table"
  []
  (let [users (re-frame/subscribe [:users])]
    (sortable-table {:class-name "user-panel"
                     :collection @users
                     :record-id-key :_id
                     :aliases {:_id "User ID"
                               :username "Username"
                               :bots "Bots"
                               :roles "User Roles"
                               :remove "Remove User"}
                     :formatters {:bots (fn [record]
                                          [:div])
                                  :remove (fn [record]
                                            [:button {:on-click #(remove-user-action record)} "Remove User"])}})))

(defn remove-game-action
  "determin if a game should be removed"
  [record]
  (re-frame/dispatch [:display-alert
                      {:title (str "Are you sure you want to remove " (:_id record) "?")
                       :confirmed #(re-frame/dispatch [:remove-game (:_id record)])
                       :type :option}]))

(defn initialize-game-action
  "initializes a game"
  [record]
  (re-frame/dispatch [:display-alert
                      {:title (str "Are you sure you want to initialize this game?")
                       :confirmed #(re-frame/dispatch [:initialize-game (:_id record)])
                       :type :option}]))

(defn start-game-action
  "starts a game"
  [record]
  (re-frame/dispatch [:display-alert
                      {:title (str "Are you sure you want to start this game?")
                       :confirmed #(re-frame/dispatch [:start-game (:_id record)])
                       :type :option}]))

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
                      :aliases {:_id "Game ID"
                                :start-game "Game Action"}
                      :formatters {:players (fn [record]
                                              [:div])
                                   :remove (fn [record]
                                             [:button {:on-click #(remove-game-action record)} "Remove Game"])
                                   :frames (fn [record]
                                             [:div])
                                   :game-action (fn [record]
                                                  (cond
                                                   (= (:state record) "pending")
                                                   [:button {:on-click #(initialize-game-action record)} "Initialize Game"]
                                                   (= (:state record) "initialized")
                                                   [:button {:on-click #(start-game-action record)} "Start Game"]))}})]))

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
