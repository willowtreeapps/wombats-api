(ns wombats.interceptors.dao)

(defn add-dao-functions
  "Attaches the dao map to context"
  [dao-map]
  {:name ::dao-interceptor
   :enter (fn [context] (assoc context ::daos dao-map))})
