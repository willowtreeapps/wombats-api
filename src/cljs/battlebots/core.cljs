(ns battlebots.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [battlebots.config :as config]
            [battlebots.handlers]
            [battlebots.subs]
            [battlebots.db :as db]
            [battlebots.views :as views]))

(when config/debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
