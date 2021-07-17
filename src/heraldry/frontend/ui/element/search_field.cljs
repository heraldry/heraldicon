(ns heraldry.frontend.ui.element.search-field
  (:require [re-frame.core :as rf]))

(defn search-field [path & {:keys [on-change]}]
  (let [current-value @(rf/subscribe [:get-value path])]
    [:div {:style {:display "inline-block"
                   :border-radius "999px"
                   :border "1px solid #ccc"
                   :padding "3px 6px"
                   :min-width "10em"
                   :max-width "20em"
                   :width "50%"
                   :margin-bottom "0.5em"}}
     [:i.fas.fa-search]
     [:input {:name "search"
              :type "text"
              :value current-value
              :autoComplete "off"
              :on-change #(let [value (-> % .-target .-value)]
                            (if on-change
                              (on-change value)
                              (rf/dispatch-sync [:set path value])))
              :style {:outline "none"
                      :border "0"
                      :margin-left "0.5em"
                      :width "calc(100% - 12px - 1.5em)"}}]]))
