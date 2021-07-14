(ns heraldry.frontend.ui.element.colours
  (:require [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn form [path]
  (let [colours @(rf/subscribe [:get-value path])]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label "Colours"]
      [:div.option
       (for [[idx [colour _]] (map-indexed vector (sort-by first colours))]
         (let [value (get colours colour)]
           ^{:key colour}
           [:<>
            [:div {:style {:display "inline-block"
                           :margin-right "0.5em"}}
             [:div.colour-preview.tooltip {:style {:background-color colour}}
              [:div.bottom {:style {:top "30px"}}
               [:h3 {:style {:text-align "center"}} colour]
               [:i]]]
             [:select {:value (util/keyword->str value)
                       :on-change #(let [selected (keyword (-> % .-target .-value))]
                                     (rf/dispatch [:set (conj path colour) selected]))
                       :style {:vertical-align "top"}}
              (for [[group-name & group-choices] attributes/tincture-modifier-for-charge-choices]
                (if (and (-> group-choices count (= 1))
                         (-> group-choices first keyword?))
                  (let [key (-> group-choices first)]
                    ^{:key key}
                    [:option {:value (util/keyword->str key)} group-name])
                  ^{:key group-name}
                  [:optgroup {:label group-name}
                   (for [[display-name key] group-choices]
                     ^{:key key}
                     [:option {:value (util/keyword->str key)} display-name])]))]]
            (when (-> idx inc (mod 2) zero?)
              [:br])]))]]]))

(defmethod interface/form-element :colours [path]
  [form path])
