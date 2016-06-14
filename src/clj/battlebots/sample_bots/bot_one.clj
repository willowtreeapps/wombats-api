(ns battlebots.sample-bots.bot-one)

;; TYPE
;;
;; Decision types descirbe at a high level what your bots
;; intensions are. Valid types are;
;;
;; 1. "MOVE"
;; 2. "SHOOT"
;; 3. "HEAL"
;;
;; TODO: Identify full decision type list

;; METADATA
;;
;; Metadata provides the detials around your decision type.
;; Example: If your decision type is "MOVE", the metadata would
;; include the property `direction`.
;;
;; TODO: Identify all metadata options for each type

;; SAVED STATE
;;
;; Saved state is state that can be persisted to the next round.

(defn run
  [arena saved-state my-id]
  {:type "MOVE"
   :metadata {:direction 1}
   :saved-state {}})
