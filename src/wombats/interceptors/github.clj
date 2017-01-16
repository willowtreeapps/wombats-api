(ns wombats.interceptors.github)

(defn add-github-settings
  "Attaches the github settings map to context"
  [github-settings]
  {:name ::github-settings
   :enter (fn [context] (assoc context ::github-settings github-settings))})

(defn get-github-settings
  "Helper method used to extract github settings from the context map"
  [context]
  (::github-settings context))
