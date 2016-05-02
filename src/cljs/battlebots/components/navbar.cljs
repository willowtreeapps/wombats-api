(ns battlebots.components.navbar
  (:require [re-frame.core :as re-frame]))

(def navbar-links [{:path "#/"           :display "Home"}
                   {:path "#/about"      :display "About"}
                   {:path "#/playground" :display "Playground"}])

(defn render-link 
  "Renders a single navbar link"
  [link]
  (fn []
    [:li.navbar-link
     [:a {:href (:path link)} (:display link)]]))

(defn root
  "Navbar container"
  []
  (fn []
    [:nav.navbar
     [:ul
      (for [link navbar-links]
        ^{:key (:path link)} [render-link link])]]))
