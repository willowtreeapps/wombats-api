(ns wombats.components.arena
  (:require [re-frame.core :as re-frame]))

(defn show-cell-details
  "Renders a cells details"
  [cell]
  (re-frame/dispatch [:display-modal [:div (str cell)]]))

(defn render-cell
  "renders a cell of a game board"
  [cell]
  (let [display (:display cell)
        md (:md cell)
        class-name (str "cell" (apply str
                                      (map (fn [md-item]
                                             (str " " (:type (last md-item)))) md)))]
    #_(if (> (count md) 0)
      (println md))
    (fn []
      (if (:_id cell)
        [:li.cell.player {:on-click #(show-cell-details cell)}]
        [:li {:class-name class-name} display]))))

(defn render-row
  "renders a row of a game board"
  [row]
  (fn []
    [:ul.row
     (for [cell row]
       ^{:key (rand 100)} [render-cell cell])]))

(defn render-player-stats
  "renders players score & details"
  [{:keys [login hp] :as player}]
  (fn []
    [:li.player-row
     [:p.name login]
     [:p.score hp]]))

;; TODO Components are not being updated so we are passing a unique key for each one...
;; this is going to be slow. Look into update components without this work around
(defn render-arena
  []
  (let [{:keys [initial-arena] :as active-game} @(re-frame/subscribe [:active-game])
        {:keys [map players messages] :as active-frame} @(re-frame/subscribe [:active-frame])
        arena (or map initial-arena)]
    [:div.active-game
     [:div.arena
      (for [row arena]
        ^{:key (rand 100)} [render-row row])]

     [:div.score-board
      [:p.header.name "Name"]
      [:p.header "HP"]
      [:ul.player-stats
       (for [player players]
         ^{:key (rand 10)} [render-player-stats player])]]
     [:div.messages
      [:ul.message
       (for [message (:global messages)]
         ^{:key (rand 10)} [:li message])]]]))
