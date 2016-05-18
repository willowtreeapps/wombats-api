(ns battlebots.routes
    (:require-macros [secretary.core :refer [defroute]])
    (:import goog.History)
    (:require [secretary.core :as secretary]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [re-frame.core :as re-frame]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")

  ;; ----- ROUTES ------
  (defroute "/" []
    (re-frame/dispatch [:set-active-panel :home-panel]))

  (defroute "/about" []
    (re-frame/dispatch [:set-active-panel :about-panel]))

  (defroute "/playground" []
    (re-frame/dispatch [:set-active-panel :playground-panel]))
  
  (defroute "/signin" []
    (re-frame/dispatch [:set-active-panel :signin-panel]))

  (defroute "/signup" []
    (re-frame/dispatch [:set-active-panel :signup-panel]))
  ;; ------- END -------
  (hook-browser-navigation!))
