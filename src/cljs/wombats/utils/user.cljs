(ns wombats.utils.user
  (:require [wombats.utils :refer [in?]]))

(defn isAdmin?
  "tests if a user object contains the admin role"
  [user]
  (:admin user))

(defn isUser?
  "tests if a user is logged in"
  [user]
  (boolean (:_id user)))
