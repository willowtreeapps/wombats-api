(ns battlebots.views.index
  (:require [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
    [:h3 "ClojureScript has not been compiled!"]
    [:p "please run "
      [:b "lein figwheel"]
      " in order to start the compiler"]])

(def index
  (html5
    [:head
      [:title "Battlebots"]
      [:meta {:charset "utf-8"
              :name "viewport"
              :content "width=device-width, initial-scale=1"}]
      (include-css (if (env :dev) "/css/main.css" "/css/main.min.css"))]
    [:body
     [:div {:id "output"}]
      mount-target
      (include-js "/js/app.js")]))
