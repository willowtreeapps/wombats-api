(ns wombats.components.ui
  (:require [re-frame.core :as re-frame]))

(defn render-modal
  [modal]
  (if modal
    [:div.modal-container
     [:div.modal-component
      [:button.clear-modal-btn {:on-click #(re-frame/dispatch [:clear-modal])} "X"]
      modal]]))

(defn render-alert
  [{:keys [title confirmed type] :as alert}]
  (if alert
    [:div.alert-container
     [:div.alert-component
      [:p.alert-title title]
      (if (= type :option)
        [:div.options
         [:button {:on-click (fn []
                               (confirmed)
                               (re-frame/dispatch [:clear-alert]))} "yes"]
         [:button {:on-click #(re-frame/dispatch [:clear-alert])} "no"]])]]))
