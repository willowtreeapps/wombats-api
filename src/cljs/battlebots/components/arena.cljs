(ns battlebots.components.arena
  (:require [re-frame.core :as re-frame]))

(defn show-cell-details
  "Renders a cells details"
  [cell]
  (re-frame/dispatch [:display-modal [:div (str cell)]]))

(defn render-cell
  "renders a cell of a game board"
  [cell]
  (fn []
    (if (:_id cell)
      [:li.cell.player {:on-click #(show-cell-details cell)}]
      [:li.cell (:display cell)])))

(defn render-row
  "renders a row of a game board"
  [row]
  (fn []
    [:ul.row
     (for [cell row]
       ^{:key (rand 100)} [render-cell cell])]))

(defn render-player-stats
  "renders players score & details"
  [{:keys [login score] :as player}]
  (fn []
    [:li.player-row
     [:p.name login]
     [:p.score score]]))

;; TODO Components are not being updated so we are passing a unique key for each one...
;; this is going to be slow. Look into update components without this work around
(defn render-arena
  []
  (let [{:keys [initial-arena] :as active-game} @(re-frame/subscribe [:active-game])
        {:keys [map players] :as active-round} @(re-frame/subscribe [:active-round])
        arena (or map initial-arena)]
    [:div.active-game
     [:div.arena
      (for [row arena]
        ^{:key (rand 100)} [render-row row])]

     [:div.score-board
      [:p.header.name "Name"]
      [:p.header "Score"]
      [:ul.player-stats
       (for [player players]
         ^{:key (rand 10)} [render-player-stats player])]]]))
