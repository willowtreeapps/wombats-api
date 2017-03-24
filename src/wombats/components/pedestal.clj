(ns wombats.components.pedestal
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [wombats.interceptors.error-logger :refer [error-logger]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.cors :as cors]
            [io.pedestal.interceptor.helpers :as interceptor]
            [io.pedestal.http.impl.servlet-interceptor :as servlet-interceptor]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.secure-headers :as sec-headers]
            [io.pedestal.http.jetty.websockets :as ws]))

;; Private helper functions

(defn- create-service-map
  [config service]
  (let [env (:env config)
        {:keys [port type
                join? container-options
                allowed-origins]} (get-in config [:settings :pedestal])
        {:keys [api-routes ws-routes]} (get-in service [:service])]
    {::env env
     ::http/resource-path "/public"
     ::http/file-path "/public"
     ::http/allowed-origins (if (contains? #{:dev :dev-ddb} env)
                              (fn [origin] true)
                              #(re-find #"\.wombats\.io|\/\/wombats.io" %))
     ::http/routes api-routes
     ::http/port port
     ::http/type type
     ::http/join? join?
     ::http/container-options (merge container-options
                                     {:context-configurator #(ws/add-ws-endpoints %
                                                                                  ws-routes)})}))

(defn- is-dev?
  "Determines if the service is running in a dev env."
  [service-map]
  (= (::env service-map) :dev))

(def log-request
  "Log the request's method and uri."
  (interceptor/on-request
    ::log-request
    (fn [request]
      (log/info (format "%s %s"
                        (clojure.string/upper-case (name (:request-method request)))
                        (:uri request)))
      request)))

(defn- default-interceptors
  "Modified version of
  https://github.com/pedestal/pedestal/blob/master/service/src/io/pedestal/http.clj#L180

  Adds interceptors to all environments"
  [service-map]
  (let [{interceptors ::http/interceptors
         routes ::http/routes
         router ::http/router
         file-path ::http/file-path
         resource-path ::http/resource-path
         method-param-name ::http/method-param-name
         allowed-origins ::http/allowed-origins
         not-found-interceptor ::http/not-found-interceptor
         ext-mime-types ::http/mime-types
         enable-session ::http/enable-session
         enable-csrf ::http/enable-csrf
         secure-headers ::http/secure-headers
         :or {router :map-tree
              method-param-name :_method
              ext-mime-types {}
              enable-session nil
              enable-csrf nil
              secure-headers {}}} service-map
        processed-routes (cond
                           (satisfies? route/ExpandableRoutes routes) (route/expand-routes routes)
                           (fn? routes) routes
                           (nil? routes) nil
                           (and (seq? routes) (every? map? routes)) routes
                           :else (throw (ex-info "Routes specified in the service map don't fulfill the contract.
                                                 They must be a seq of full-route maps or satisfy the ExpandableRoutes protocol"
                                                 {:routes routes})))]

    (assoc service-map ::http/interceptors
           (cond-> []
             true (conj error-logger)
             true (conj log-request)
             true (conj (cors/allow-origin allowed-origins))
             true (conj http/not-found)
             (or enable-session enable-csrf) (conj (middlewares/session (or enable-session {})))
             enable-csrf (conj (csrf/anti-forgery enable-csrf))
             true (conj (middlewares/content-type {:mime-types ext-mime-types}))
             true (conj route/query-params)
             true (conj (route/method-param method-param-name))
             true (conj (middlewares/resource resource-path))
             true (conj (middlewares/file file-path))
             (not (nil? secure-headers)) (conj (sec-headers/secure-headers secure-headers))
             true (conj (route/router processed-routes router))))))

(defn- dev-interceptors
  [service-map]
  (update-in service-map [::http/interceptors]
             #(vec (->> %
                        (cons cors/dev-allow-origin)
                        (cons servlet-interceptor/exception-debug)))))

(defn- start-http-server
  [service-map]
  (cond-> service-map
    true (default-interceptors)
    (is-dev? service-map) (dev-interceptors)
    true (http/create-server)
    true (http/start)))

(defn- log-connection-information
  "Logs connection information"
  [service-map]
  (when-let [_ (is-dev? service-map)]
    (log/info (str "Pedestal service is running on port "
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
