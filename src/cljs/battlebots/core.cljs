(ns battlebots.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [battlebots.handlers.root]
            [battlebots.subs.root]
            [battlebots.routes :as routes]
            [battlebots.views :as views]
            [battlebots.config :as config]))

(when config/debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
