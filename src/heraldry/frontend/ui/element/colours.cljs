(ns heraldry.frontend.ui.element.colours
  (:require [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.strings :as strings]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn form [path]
  (let [colours @(rf/subscribe [:get-value path])]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label [tr {:en "Colours"
                   :de "Farben"}]]
      [:div.option
       (if (seq colours)
         (doall
          (for [[colour _] (sort-by first colours)]
            (let [value (get colours colour)
                  [value qualifier] (if (vector? value)
                                      value
                                      [value :none])]
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
                 (doall
                  (for [[group-name & group-choices] attributes/tincture-modifier-for-charge-choices]
                    (if (and (-> group-choices count (= 1))
                             (-> group-choices first keyword?))
                      (let [key (-> group-choices first)]
                        ^{:key key}
                        [:option {:value (util/keyword->str key)}
                         (tr group-name)])
                      ^{:key group-name}
                      [:optgroup {:label (tr group-name)}
                       (doall
                        (for [[display-name key] group-choices]
                          ^{:key key}
                          [:option {:value (util/keyword->str key)}
                           (tr display-name)]))])))]
                " "
                (when-not (#{:keep
                             :outline
                             :shadow
                             :highlight
                             :layer-separator} value)
                  [:select {:value qualifier
                            :on-change #(let [selected (keyword (-> % .-target .-value))]
                                          (rf/dispatch [:set (conj path colour) (if (= selected :none)
                                                                                  value
                                                                                  [value selected])]))
                            :style {:vertical-align "top"}}
                   (doall
                    (for [[group-name & group-choices] attributes/tincture-modifier-qualifier-choices]
                      (if (and (-> group-choices count (= 1))
                               (-> group-choices first keyword?))
                        (let [key (-> group-choices first)]
                          ^{:key key}
                          [:option {:value (util/keyword->str key)}
                           (tr group-name)])
                        ^{:key group-name}
                        [:optgroup {:label (tr group-name)}
                         (doall
                          (for [[display-name key] group-choices]
                            ^{:key key}
                            [:option {:value (util/keyword->str key)}
                             (tr display-name)]))])))])]
               [:br]])))
         [tr strings/none])]]]))

(defmethod interface/form-element :colours [path]
  [form path])
