(ns battlebots.components.arena
  (:require [re-frame.core :as re-frame]))

(defn show-cell-details
  "Renders a cells details"
  [cell]
  (println cell)
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

(defn render-arena
  [game round]
  (let [arena (if (empty? round)
                (:initial-arena game)
                round)]
    [:div.active-game
     (for [row arena]
       ^{:key (rand 100)} [render-row row])]))
