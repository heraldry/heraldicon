(ns heraldry.frontend.language
  (:require [re-frame.core :as rf]))

(def language-path
  [:ui :language])

(rf/reg-sub ::selected-language
  (fn [db _]
    (or (get-in db language-path)
        :en)))

(rf/reg-sub ::selector-opacity
  (fn [_ _]
    (rf/subscribe [::selected-language]))

  (fn [selected-language [_ language]]
    (if (= selected-language language)
      1
      0.25)))

(defn selector []
  [:div {:style {:white-space "nowrap"
                 :position "relative"
                 :top "50%"
                 :transform "translate(0,-50%)"}}
   [:img {:src "/img/flag-united-kingdom.svg"
          :on-click #(rf/dispatch [:set language-path :en])
          :style {:width "2em"
                  :height "1em"
                  :cursor "pointer"
                  :opacity @(rf/subscribe [::selector-opacity :en])}}]
   " "
   [:img {:src "/img/flag-germany.svg"
          :on-click #(rf/dispatch [:set language-path :de])
          :style {:width "2em"
                  :height "1em"
                  :cursor "pointer"
                  :opacity @(rf/subscribe [::selector-opacity :de])}}]])

(defn tr [data]
  (get data
       @(rf/subscribe [::selected-language])
       (get data :en)))
