(ns wombats.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [wombats.handlers.root]
            [wombats.subs.root]
            [wombats.routes :as routes]
            [wombats.views :as views]
            [wombats.config :as config]))

(when config/debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-app])
  (re-frame/dispatch [:bootstrap-app])
  (mount-root))
