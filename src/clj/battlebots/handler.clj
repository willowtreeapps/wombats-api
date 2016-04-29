(ns battlebots.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [battlebots.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [battlebots.dao :as dao]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def loading-page
  (html5
   [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))]
    [:body
     mount-target
     (include-js "/js/app.js")]))


(defroutes
  routes
  ;; CMS home page will live here
  (GET "/" [] loading-page)

  (resources "/")
  (not-found "Not Found")

  ;; list games
  (GET "/games" [] (dao/get-games))
  )

(def app (wrap-middleware #'routes))
