(ns heraldicon.frontend.element.search-field
  (:require
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defn search-field [context & {:keys [on-change]}]
  (let [current-value (interface/get-raw-data context)]
    [:div.search-field
     [:i.fas.fa-search]
     [:input {:name "search"
              :type "search"
              :value current-value
              :autoComplete "off"
              :on-change #(let [value (-> % .-target .-value)]
                            (if on-change
                              (on-change value)
                              (rf/dispatch-sync [:set context value])))
              :style {:outline "none"
                      :border "0"
                      :margin-left "0.5em"
                      :width "calc(100% - 12px - 1.5em)"}}]]))
