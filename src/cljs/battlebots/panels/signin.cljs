(ns battlebots.panels.signin
  (:require [re-frame.core :as re-frame]
            [reagent.core :refer [atom]]))

(def state (atom {:doc {:username ""
                        :password ""}
                  :errors []
                  :submitted? false}))

(defn set-value! [id value]
  (swap! state assoc :submitted? false)
  (swap! state assoc-in [:doc id] value))

(defn get-value [id]
  (get-in @state [:doc id]))

(defn is-valid? []
  (if (or (empty? (get-value :username))
          (empty? (get-value :password)))
    false
    true))

(defn on-submit []
  (if (is-valid?)
    (do 
      (swap! state assoc :submitted? true)
      (re-frame/dispatch [:sign-in (get-value :doc)]))))

(defn signin-panel []
  (fn []
    [:div.panel-signin
     [:h1 "Signin"]
     [:p (if (:submitted? @state)
             "Submitted"
             "Pending...")]
     [:input {:type "text"
              :value (get-value :username)
              :on-change #(set-value! :username (-> % .-target .-value))
              :placeholder "username"}]
     [:input {:type "password"
              :on-change #(set-value! :password (-> % .-target .-value))
              :placeholder "password"}]
     [:input {:type "button"
              :value "signin"
              :on-click on-submit}]]))
