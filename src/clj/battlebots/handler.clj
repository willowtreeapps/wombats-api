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
     (include-css (if (env :dev) "/css/main.css" "/css/main.min.css"))]
    [:body
     mount-target
     (include-js "/js/app.js")]))


(defroutes
  routes
  ;; CMS home page will live here
  (GET "/" [] loading-page)

  (resources "/")
  (not-found "Not Found")

  ;; add round - should return a URI to identify round
  (POST "/rounds" [round])

  ;; list rounds
  (GET "/rounds" [] (dao/get-rounds))

  ;; add player - should return a URI to identify player
  (POST "/rounds/:round-id/players/" [round-id player])

  ;; remove player
  (DELETE "/rounds/:round-id/players/:player-id" [round-id player-id])


  ;; get arena
  ;; TODO - websockets call to push all arena changes out to listeners


  )

(def app (wrap-middleware #'routes))
