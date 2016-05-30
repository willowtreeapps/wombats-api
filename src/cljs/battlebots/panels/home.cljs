(ns battlebots.panels.home
  (:require [re-frame.core :as re-frame]))

(defn battlebot-game
  "renders available games"
  [game]
  (fn []
    [:li
     [:button {:on-click #(re-frame/dispatch [:set-active-game game])}
      (:_id game)]]))

(defn battlebot-board-cell
  "renders a cell of a game board"
  [cell]
  (fn []
    [:li.cell cell]))

(defn battlebot-board-row
  "renders a row of a game board"
  [row]
  (fn []
    [:ul.row
     (for [cell row]
       ^{:key (rand 100)} [battlebot-board-cell cell])]))

(defn authed-homepage
  [user games active-game]
  [:div
   [:h3 (str "Welcome back " (:username @user) "!")]
   [:p "Game ids"]
   [:ul.game-list
    (for [game @games]
      ^{:key (:_id game)} [battlebot-game game])]
   [:div.active-game
    (for [row (:initial-arena @active-game)]
      ^{:key (rand 100)} [battlebot-board-row row])]])

(def unauthed-homepage
  [:div
   [:p
    [:a {:href "#/signup"} "Signup"]
    " or "
    [:a {:href "#/signin"} "Signin"]
    " now!"]])

(defn home-panel []
  (re-frame/dispatch [:fetch-games])
  (let [active-game (re-frame/subscribe [:active-game])
        games (re-frame/subscribe [:games])
        user (re-frame/subscribe [:user])]
    (fn []
      [:div.panel-home
       [:h1 "Battlebots"]
       (if @user
         (authed-homepage user games active-game)
         unauthed-homepage)])))
