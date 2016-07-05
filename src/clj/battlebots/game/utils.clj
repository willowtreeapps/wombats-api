(ns battlebots.game.utils
  (:require [battlebots.constants.arena :refer [arena-key]]))

;; Modified from
;; http://stackoverflow.com/questions/4830900/how-do-i-find-the-index-of-an-item-in-a-vector
(defn position
  [pred coll]
  (first (keep-indexed (fn [idx x]
                         (when (pred x)
                           idx))
                       coll)))

(defn is-player?
  "Checks to see if an item is a player"
  [item]
  (boolean (:login item)))

(defn get-player
  "Retrieves a player from the private player collection"
  [player-id players]
  (first (filter #(= (:_id %) player-id) players)))

(defn update-player-with
  "Updates a player in the private player collection"
  [player-id players update]
  (map #(if (= player-id (:_id %))
          (merge % update)
          %) players))

(defn apply-player-update
  "Applies an update to a player object"
  [player update]
  (reduce (fn [player [prop update-fn]]
            (assoc player prop (update-fn (get player prop)))) player update))

(defn modify-player-stats
  "maps over all players and applies an update if the pred matches"
  [player-id update players]
  (map (fn [{:keys [_id] :as player}]
         (if (= player-id _id)
           (apply-player-update player update)
           player)) players))

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
  (select-keys player [:_id :uuid :login :energy]))

(defn apply-damage
  "applies damage to items that have energy. If the item does not have energy, return the item.
  If the item after receiving damage has 0 or less energy, replace it with an open space"
  ([item damage] (apply-damage item damage true))
  ([{:keys [energy] :as item} damage replace-item?]
   (if energy
     (let [updated-energy (- energy damage)
           updated-item (assoc item :energy updated-energy)
           destroyed? (>= 0 updated-energy)]
       (if (and destroyed? replace-item?)
         (:open arena-key)
         updated-item))
     item)))
