(ns wombats.panels.home
  (:require [re-frame.core :as re-frame]
            [wombats.services.forms :as f]
            [wombats.components.arena :refer [render-arena]]))


(defn is-valid?
  [form]
  (boolean (f/get-value :repo form)))

(defn register-user
  [game-id {:keys [bots _id] :as user}]
  (let [form (f/initialize {:repo nil})]
    [:div
     (for [bot bots]
       ^{:key (:repo bot)} [:div
                            [:input {:type "radio"
                                     :name "bot"
                                     :value (:repo bot)
                                     :on-change #(f/set-value! :repo (-> % .-target .-value) form)}]
                            [:p (:name bot)]])
     [:input {:type "button"
              :value "Join Game"
              :on-click #(f/on-submit {:form form
                                       :validator is-valid?
                                       :dispatch [:register-user-in-game
                                                  game-id
                                                  _id
                                                  (get-in @form [:doc :repo])]})}]]))

(defn player-modal
  [players]
  [:div
   [:ul.player-list
    (for [player players]
      ^{:key (:_id player)} [:li.player (:login player)])]])

(defn battlebot-game
  "renders available games"
  [{:keys [players state] :as game} user]
  (let [game-id (:_id game)
        user-id (:_id user)
        is-registered? (boolean (first (filter #(= user-id (:_id %)) players)))]
    (fn []
      [:li
       [:button {:on-click #(re-frame/dispatch [:set-active-game game])} game-id]
       (cond
        (and is-registered? (or (= state "pending") (= state "initialized")))
        [:button {:on-click #(re-frame/dispatch [:display-modal (player-modal players)])} "View Players"]

        (and (not is-registered?) (= state "pending"))
        [:button {:on-click #(re-frame/dispatch [:display-modal (register-user game-id user)])} "Join Game"]

        (= state "finalized")
        [:button {:on-click #(re-frame/dispatch [:play-game game-id])} "Play Game"])])))

(defn authed-homepage
  [user games]
  [:div
   [:h3 (str "Welcome back " (:login user) "!")]
   [:p "Game ids"]
   [:ul.game-list
    (doall (for [game games]
             ^{:key (str (:_id game) "-" (count (:players game)))} [battlebot-game game user]))]
   (render-arena)])

(def unauthed-homepage
  [:div
   [:p
    [:a {:href "/signin/github"} "Signup"]
    " and get started!"]])

(defn home-panel []
  (re-frame/dispatch [:fetch-games])
  (let [games (re-frame/subscribe [:games])
        user (re-frame/subscribe [:user])]
    (fn []
      [:div.panel-home
       (if @user
         (authed-homepage @user @games)
         unauthed-homepage)])))
