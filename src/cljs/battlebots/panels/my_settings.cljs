(ns battlebots.panels.my-settings
  (:require [re-frame.core :as re-frame]
            [battlebots.services.forms :as f]))

(defn is-valid? [form]
  (if (empty? (f/get-value :repo form))
    false
    true))

(defn remove-bot-action
  [{:keys [repo name]}]
  (re-frame/dispatch [:display-alert
                      {:title (str "Are you sure you want to remove " name "?")
                       :confirmed #(re-frame/dispatch [:remove-bot repo])
                       :type :option}]))

(defn render-bot-list
  [bots]
  [:ul.bot-list
   [:li.bot
    [:p.bot-name.header "Bot Name"]
    [:p.bot-repo.header "Repo Name"]]
   (for [bot bots]
     ^{:key (:repo bot)} [:li.bot
                          [:p.bot-name (:name bot)]
                          [:p.bot-repo (:repo bot)]
                          [:button.remove-bot-btn {:on-click #(remove-bot-action bot)} "Remove Bot"]])])

(defn my-settings-panel []
  (let [user (re-frame/subscribe [:user])
        form (f/initialize {:repo ""
                            :name ""})]
    (fn []
      [:div.panel-my-settings
       [:h2 (:login @user)]
       [:img {:src (:avatar_url @user)
              :alt (str (:login @user) "'s avatar")
              :class-name "avatar"}]

       (render-bot-list (:bots @user))

       [:p.header "Settings"]

       ;; Add Bot
       [:p "Add Bot"]
       [:input {:type "text"
                :value (f/get-value :name form)
                :on-change #(f/set-value! :name (-> % .-target .-value) form)
                :placeholder "Bot Name"}]
       [:input {:type "text"
                :value (f/get-value :repo form)
                :on-change #(f/set-value! :repo (-> % .-target .-value) form)
                :placeholder "Repo Name"}]
       [:input {:type "button"
                :value "Add Bot"
                :on-click #(f/on-submit {:form form
                                         :validator is-valid?
                                         :dispatch [:add-bot (:doc @form) (:_id @user)]})}]])))
