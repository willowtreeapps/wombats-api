(ns battlebots.db
  (:require [alandipert.storage-atom :refer [local-storage]]))

;; Set account / user information to localstorage
(def account (local-storage (atom {}) :account))

;; Set auth-token
(def auth-token (local-storage (atom {}) :auth-token))

(def default-db
  {:bootstrapping? false
   :auth-token auth-token
   :account account
   :active-game {}
   :games []
   :errors []
   :word "Aloha"})
