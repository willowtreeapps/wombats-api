(ns wombats-api.game.utils
  (:require [wombats-api.constants.arena :refer [arena-key]]))

;; Modified from
;; http://stackoverflow.com/questions/4830900/how-do-i-find-the-index-of-an-item-in-a-vector
(defn position
  [pred coll]
  (first (keep-indexed (fn [idx x]
                         (when (pred x) idx)) coll)))

(defn is-player?
  "Checks to see if an item is a player"
  [item]
  (= (:type item) "player"))

(defn is-open?
  "Checks to see if an item is a player"
  [item]
  (= (:type item) "open"))

(defn get-player
  "Retrieves a player from the private player collection"
  [player-id players]
  (first (filter #(= (:_id %) player-id) players)))

(defn update-with
  [cell update-map]
  (reduce-kv (fn [m k update-fn]
               (if (and (contains? m k) update-fn)
                 (update-in m [k] update-fn)
                 m))
             cell
             update-map))

(defn update-player-with

  "Updates a player in the private player collection with an object.

  NOTE: This will overwrite previous values with a merge. If you don't know
  the value you want to modify but know how you want to modify it, use
  `modify-player-stats`"
  [player-id players update]
  (map #(if (= player-id (:_id %))
          (merge % update)
          %) players))

(defn modify-player-stats
  "maps over player collection and applies an update to the matching player.

  ex update: {:hp #(+ % 10)
              :something-other-player-prop #(* % 5)}"
  [player-id update players]
  (map
   (fn [{:keys [_id] :as player}]
     (if (= player-id _id)
       (reduce (fn [player [prop update-fn]]
                 (assoc player prop (update-fn (get player prop))))
               player
               update)
       player))
   players))

(defn get-item-coords
  "Returns a tuple of a given players coords

  TODO: There's most likely a better way to accomplish this"
  [uuid arena]
  (:coords (reduce (fn [memo row]
                     (if (:coords memo)
                       memo
                       (let [idx (position #(= (:uuid %) uuid) row)
                             row-number (:row memo)]
                         (if idx
                           {:row (+ 1 row-number)
                            :coords [idx row-number]}
                           {:row (+ 1 row-number)}))))
                   {:row 0} arena)))

(defn get-player-coords
  "Returns a tuple of a given players coords

  TODO: There's most likely a better way to accomplish this"
  [_id arena]
  (:coords (reduce (fn [memo row]
                     (if (:coords memo)
                       memo
                       (let [idx (position #(= (:_id %) _id) row)
                             row-number (:row memo)]
                         (if idx
                           {:row (+ 1 row-number)
                            :coords [idx row-number]}
                           {:row (+ 1 row-number)}))))
                   {:row 0} arena)))

(defn sanitize-player
  "Sanitizes the full player object returning the partial used on the game map"
  [player]
  (select-keys player [:_id :uuid :login :hp :type]))

(defn apply-damage
  "applies damage to items that have hp. If the item does not have hp, return the item.
  If the item after receiving damage has 0 or less hp, replace it with an open space"
  ([item damage] (apply-damage item damage true))
  ([{:keys [hp] :as item} damage replace-item?]
   (if hp
     (let [updated-hp (- hp damage)
           updated-item (assoc item :hp updated-hp)
           destroyed? (>= 0 updated-hp)]
       (if (and destroyed? replace-item?)
         (:open arena-key)
         updated-item))
     item)))
