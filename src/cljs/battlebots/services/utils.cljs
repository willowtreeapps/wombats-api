(ns battlebots.services.utils)

;;
;; Local Storage
;;
;; https://gist.github.com/daveliepmann/cf923140702c8b1de301
(defn set-item!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  (.getItem (.-localStorage js/window) key))

(defn remove-item!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  (.removeItem (.-localStorage js/window) key))

;;
;; Service Helpers
;;
(defn get-auth-header 
  "returns an Authorization header if one should be present" 
  []
  {:Authorization (str "Token " (get-item "token"))})

(defn add-auth-header
  "Add token to header"
  [headers]
  (merge (get-auth-header) headers))
