(ns battlebots.components.navbar
  (:require [re-frame.core :as re-frame]))

(def authenticated-links [{:path "#/signout" :display "Sign out"}])

(def unauthenticated-links [{:path "#/signin" :display "Signin"}])

(def common-links [{:path "#/"           :display "Home"}
                   {:path "#/about"      :display "About"}
                   {:path "#/playground" :display "Playground"}])

(defn resolve-links
  "renders role dependent links"
  [isAuthed?]
  (if isAuthed?
    (concat common-links authenticated-links)
    (concat common-links unauthenticated-links)))

(defn render-link
  "Renders a single navbar link"
  [link]
  (fn []
    [:li.navbar-link
     [:a {:href (:path link)} (:display link)]]))

(defn root
  "Navbar container"
  []
  (let [token (re-frame/subscribe [:auth-token])]
    (fn []
      [:nav.navbar
       [:ul
        (for [link (resolve-links (not (empty? @token)))]
          ^{:key (:path link)} [render-link link])]])))
