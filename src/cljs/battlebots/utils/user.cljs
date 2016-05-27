(ns battlebots.utils.user
  (:require [battlebots.utils :refer [in?]]))

(defn isAdmin?
  "tests if a user object contains the admin role"
  [user]
  (in? (:roles user) "admin"))

(defn isUser?
  "tests if a user is logged in"
  [user]
  (in? (:roles user) "user"))
