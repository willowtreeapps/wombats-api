(ns battlebots.panels.signin)

(defn signin-panel []
  (fn []
    [:div.panel-signin
     [:h1 "Signin"]
     [:input {:type "text"
              :placeholder "email"}]
     [:input {:type "password"
              :placeholder "password"}]
     [:input {:type "button"
              :value "signin"}]]))
