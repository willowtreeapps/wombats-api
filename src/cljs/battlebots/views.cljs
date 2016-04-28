(ns battlebots.views
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]))

(defn main-panel
  []
  (let [word (subscribe [:word])]
    (fn []
      [:div
       [:h1 "The word is " @word]
       [:input {:type "text"
                :value @word
                :on-change (fn [e]
                             (dispatch [:update-word (-> e .-target .-value)]))}]])))

