(ns wombats.handlers.helpers
  (:require [wombats.constants :refer [errors]]
            [cemerick.url :refer [url url-encode]]))

(defn wombat-error
  "Throws an error that will be caught by the exception interceptor."
  [{code :code
    details :details
    params :params
    message :message
    field-error :field-error
    :or {code 1
         details {}
         params []
         message nil
         field-error nil}}]

  (let [message (or message
                    (get errors code "Oops, looks like something went wrong."))]
    (throw (ex-info "Wombat Error" (cond-> {:type :wombat-error
                                            :message (->> params
                                                          (into [message])
                                                          (apply format))
                                            :details details
                                            :code code}
                                     (not (nil? field-error))
                                     (merge {:field-error field-error}))))))

(defn- format-link
  [link]
  (let [query (:query (url link))
        page-number-string (get query "page")]
    {:query query
     :page-number (when page-number-string
                    (Integer/parseInt page-number-string))
     :link link}))

(defn- format-links
  [links]
  (apply merge
   (map (fn [link]
          (let [[ln rel] (clojure.string/split link #";")]
            (when (and ln rel)
              {(keyword (last (re-find #"rel=\"(.*)\"" rel)))
               (format-link (last (re-find #"<(.*)>" link)))})))
        links)))

(defn parse-link-headers
  "Parses the link header into a usable data structure.

  Ex: [{:query {}
        :page-number 0
        :link \"http://...\"}]

  NOTE: This could be used put into a cljc file and used on the client as well"
  [context]
  (let [link-header (get-in context [:request :headers "link"])]
    (when link-header
      (-> link-header
          (clojure.string/split #",")
          (format-links)))))

(defn- join-pair [k v] (str (name k) "=" (url-encode v)))

(defn- format-query
  [query]
  (->> query
   (map
    (fn [[k v]]
      (if (string? v)
        (join-pair k v)
        (map #(join-pair k %) v))))
   (flatten)
   (clojure.string/join "&")))

(defn format-url
  [base uri query]
  (str base uri "?" (format-query query)))

(defn- generate-link-headers
  [current-page last-page query url-formatter]
  (cond-> [{:link (url-formatter (assoc query :page "0"))
            :rel "first"}
           {:link (url-formatter (assoc query :page (str last-page)))
            :rel "last"}]
    (> current-page 0)
    (conj {:link (url-formatter (assoc query :page (str (dec current-page))))
           :rel "prev"})

    (< current-page last-page)
    (conj {:link (url-formatter (assoc query :page (str (inc current-page))))
           :rel "next"})))

(defn- format-link-headers
  [{headers :headers
    uri :uri
    query :query-params} current-page last-page]
  (let [origin (get headers "origin")
        link-headers (generate-link-headers current-page
                                            last-page
                                            query
                                            (partial format-url origin uri))]
    (->> link-headers
     (map (fn [{:keys [link rel]}]
            (str "<" link ">; rel=\"" rel "\"")))
     (clojure.string/join ", "))))

(defn paginate-response
  "Wraps over a response, paginating the data passed to it. If
  the data-pred propery is passed, data will be filtered / sorted
  prior to pagination."
  [{request :request
    response :response
    response-data :response-data
    page-number-string :page-number
    per-page :per-page
    data :data
    data-pred :data-pred
    :or {response-data {:status 200}
         data-pred identity}}]

  (let [page-number (Integer/parseInt (or page-number-string "0"))
        formatted-data (vec (data-pred data))
        total-records (count formatted-data)
        total-pages (-> (/ total-records per-page)
                        (Math/ceil)
                        (int)
                        (dec))
        start-record (Math/min (* page-number per-page) total-records)
        end-record (Math/min (+ start-record per-page) total-records)
        paginated-data (subvec formatted-data start-record end-record)]
    (-> response
        (merge response-data)
        (assoc :body paginated-data)
        (assoc-in [:headers "Link"] (format-link-headers request
                                                         page-number
                                                         total-pages)))))
