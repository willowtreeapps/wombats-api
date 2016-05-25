(ns battlebots.panels.signup
  (:require [battlebots.services.forms :as f]))

(defn is-valid? [form]
  (if (or (empty? (f/get-value :username form))
          (empty? (f/get-value :password form)))
    false
    true))

(defn signup-panel []
  (let [form (f/initialize {:username ""
                            :password ""
                            :bot-repo ""})]
    (fn []
      [:div.panel-signup
       [:h1 "Signup"]
       [:p (if (:submitted? @form)
             "Submitting"
             "")]
       [:input {:type "text"
                :value (f/get-value :username form)
                :on-change #(f/set-value! :username (-> % .-target .-value) form)
                :placeholder "username"}]
       [:input {:type "password"
                :value (f/get-value :password form)
                :on-change #(f/set-value! :password (-> % .-target .-value) form)
                :placeholder "password"}]
       [:p "Github Bot Repo: ex \"oconn/battlebot\""]
       [:input {:type "text"
                :value (f/get-value :bot-repo form)
                :on-change #(f/set-value! :bot-repo (-> % .-target .-value) form)
                :placeholder "bot repo"}]
       [:input {:type "submit"
                :value "Signup"
                :on-click #(f/on-submit {:form form
                                         :validator is-valid?
                                         :dispatch :sign-up})}]])))
