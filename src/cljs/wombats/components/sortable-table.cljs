(ns wombats.components.sortable-table)

(defn get-headers
  "takes a vector of maps returns a union of all the keys"
  [collection {:keys [aliases formatters] :as config}]
  (let [initial-headers (reduce #(into %1 (keys %2)) #{} collection)
        headers (into initial-headers (set (keys formatters)))]
    (map (fn [header] {:key header
                       :display (name (or (get aliases header)
                                          header))}) headers)))

(defn render-record-item
  "renders a single item in a record"
  [item]
  (if (or
       (= [] item)
       (= {} item))
    " - "
    item))

(defn render-record
  "renders a single record"
  [record headers formatters]
  (for [header headers]
    ^{:key (:key header)} [:td (let [formatter (get formatters (:key header))]
                                 (if formatter
                                   (render-record-item (formatter record))
                                   (render-record-item (get record (:key header)))))]))

(defn render-records
  "renders all record"
  [headers {:keys [collection record-id-key formatters] :as config}]
  (for [record collection]
    ^{:key ((or record-id-key :_id) record)} [:tr (render-record record headers formatters)]))

(defn render-headers
  "renders each header"
  [headers]
  (for [header headers]
    ^{:key (:key header)} [:th (:display header)]))

(defn sortable-table
  "sortable table component"
  [{:keys [collection class-name] :as config}]
  (let [headers (get-headers collection config)]
    [:table {:class-name class-name}
     [:thead
      [:tr (render-headers headers)]]
     [:tbody (render-records headers config)]]))
