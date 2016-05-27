(ns battlebots.components.navbar
  (:require [re-frame.core :as re-frame]
            [battlebots.utils.user :refer [isAdmin? isUser?]]))

(def admin-links [{:path "#/admin" :display "Admin Center"}])

(def authenticated-links [{:path "#/signout" :display "Sign out"}])

(def unauthenticated-links [{:path "#/signin" :display "Signin"}])

(def common-links [{:path "#/"           :display "Home"}
                   {:path "#/about"      :display "About"}])

(defn resolve-links
  "renders role dependent links"
  [user]
  (cond
   (isAdmin? user) (concat common-links admin-links authenticated-links)
   (isUser? user) (concat common-links authenticated-links)
   :else (concat common-links unauthenticated-links)))

(defn render-link
  "Renders a single navbar link"
  [link]
  (fn []
    [:li.navbar-link
     [:a {:href (:path link)} (:display link)]]))

(defn root
  "Navbar container"
  []
  (let [user (re-frame/subscribe [:user])]
    (fn []
      [:nav.navbar
       [:ul
        (for [link (resolve-links @user)]
          ^{:key (:path link)} [render-link link])]])))
