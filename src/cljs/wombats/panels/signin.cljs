(ns wombats.panels.signin
  (:require [wombats.services.forms :as f]))

(defn is-valid? [form]
  (if (or (empty? (f/get-value :username form))
          (empty? (f/get-value :password form)))
    false
    true))

(defn signin-panel []
  (let [form (f/initialize {:username ""
                            :password ""})]
    (fn []
      [:div.panel-signin
       [:h1 "Signin"]
       [:p "Need an account?"
        [:a {:href "#/signup"} "Sign up here."]]
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
       [:input {:type "button"
                :value "signin"
                :on-click #(f/on-submit {:form form
                                         :validator is-valid?
                                         :dispatch :sign-in})}]])))
