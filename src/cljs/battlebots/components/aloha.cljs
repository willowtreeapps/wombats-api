(ns battlebots.components.aloha
  (:require [re-frame.core :as re-frame]))

(defn root []
  (let [word (re-frame/subscribe [:word])]
    (fn []
      [:div.component-example
       [:p "The word is " @word]
       [:input {:type "text"
                :value @word
                :on-change (fn [e]
                             (re-frame/dispatch [:update-word (-> e .-target .-value)]))}]])))
