(ns heraldry.frontend.ui.form.helms
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as ui-interface]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option []]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/helms [path]
  (let [helms-path (conj path :elements)
        num-helms @(rf/subscribe [:get-list-size helms-path])]
    {:title "Helms and crests"
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p "This feature is incomplete and likely going to change, so use with caution. :)"]]]
     :buttons [{:icon "fas fa-plus"
                :handler #(state/dispatch-on-event % [:add-element helms-path default/helm])}]
     :nodes (->> (range num-helms)
                 (map (fn [idx]
                        (let [helm-path (conj helms-path idx)]
                          {:path helm-path
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :tooltip "move down"
                                      :handler #(state/dispatch-on-event % [:move-element helm-path (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-helms))
                                      :tooltip "move up"
                                      :handler #(state/dispatch-on-event % [:move-element helm-path (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :tooltip "remove"
                                      :handler #(state/dispatch-on-event % [:remove-element helm-path])}]}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/helms [_path]
  {:form form})
