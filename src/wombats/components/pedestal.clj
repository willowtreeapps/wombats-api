(ns wombats.components.pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.jetty.websockets :as ws]))

;; Private helper functions

(defn- create-service-map
  [config service]
  (let [env (:env config)
        {:keys [port type
                join? container-options]} (get-in config [:settings :pedestal])
        {:keys [api-routes ws-routes]} (get-in service [:service])]
    {:env env
     ::http/routes api-routes
     ::http/port port
     ::http/type type
     ::http/join? join?
     ::http/container-options (merge container-options
                                     {:context-configurator #(ws/add-ws-endpoints %
                                                                                  ws-routes)})}))

(defn- is-dev?
  "Determins if the service is running in a dev env."
  [service-map]
  (= (:env service-map) :dev))

(defn- start-http-server
  [service-map]
  (cond-> service-map
    true http/default-interceptors
    (is-dev? service-map) http/dev-interceptors
    true (http/create-server)
    true (http/start)))

(defn- log-connection-information
  "Logs connection information"
  [service-map]
  (when-let [_ (is-dev? service-map)]
    (prn (str "Pedestal service is running on port "
              (::http/port service-map))))
  service-map)

;; Component

(defrecord Pedestal [config service server]
  component/Lifecycle
  (start [component]
    (if server
      component
      (assoc component :server (-> (create-service-map config service)
                                   (log-connection-information)
                                   (start-http-server)))))
  (stop [component]
    (if-not server
      component
      (do
        (http/stop server)
        (assoc component :server nil)))))

;; Public component methods

(defn new-pedestal
  []
  (map->Pedestal {}))
