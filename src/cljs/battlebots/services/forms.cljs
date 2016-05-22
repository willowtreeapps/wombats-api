(ns battlebots.services.forms
    (:require [re-frame.core :as re-frame]
              [reagent.core :refer [atom]]))

;; 
;; Form Helpers
;;
(defn initialize 
  "initializes a form object"
  [form-state]
  (atom {:doc form-state
         :errors []
         :submitted? false}))

(defn on-submit 
  "handles form submission"
  [{:keys [form validator dispatch]}]
  (let [dispatch-fn #(re-frame/dispatch [dispatch (:doc @form)])]
    (if validator
      (when (validator form)
        (dispatch-fn))
      (dispatch-fn))))

(defn set-value! [id value form]
  (swap! form assoc :submitted? false)
  (swap! form assoc-in [:doc id] value))

(defn get-value [id form]
  (get-in @form [:doc id]))
