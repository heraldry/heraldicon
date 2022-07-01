(ns heraldicon.frontend.tooltip
  (:require
   [heraldicon.frontend.language :refer [tr]]))

(defn- tooltip [message & {:keys [element width class center? style]
                           :or {element [:i.ui-icon.fas.fa-question-circle]
                                class "info"}}]
  (when message
    [:div.tooltip {:class class
                   :style style}
     element
     [:div.bottom {:style {:width width}}
      [:i]
      (cond
        (vector? message) message
        center? [:h3 {:style {:text-align "center"}} [tr message]]
        :else [:p [tr message]])]]))

(defn info [message & {:as options}]
  [tooltip message (assoc options
                          :class "info"
                          :style {:display "inline-block"
                                  :margin-left "0.2em"
                                  :vertical-align "top"})])

(defn choice [message element & {:as options}]
  [tooltip message (assoc options
                          :class "choice"
                          :element element
                          :center? true)])
