(ns battlebots.panels.signup)

(defn signup-panel []
  (fn []
    [:div.panel-signup
     [:h1 "Signup"]
     [:input {:type "text"
              :placeholder "email"}]
     [:input {:type "password"
              :placeholder "password"}]
     [:input {:type "submit"
              :value "Signup"}]]))
