(ns battlebots.panels.playground
  (:require [re-frame.core :as re-frame]
            [battlebots.components.aloha :as aloha]))

(defn root-panel []
  (fn []
    [:div.panel-playground
     [:h1 "Playground"]
     [:p.directions "Use this area of the application to host live examples of your components. Wrap each component in a \"div\" with the \"component-example\" class"]
     [:code  "[:div.component-example]"]
     [:div.playground-components
      [aloha/root []]]]))
