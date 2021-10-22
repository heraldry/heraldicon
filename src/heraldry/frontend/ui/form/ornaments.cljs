(ns heraldry.frontend.ui.form.ornaments
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.shield-separator :as shield-separator]
            [heraldry.strings :as strings]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option []]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/ornaments [path]
  (let [elements-path (conj path :elements)
        num-elements @(rf/subscribe [:get-list-size elements-path])]
    {:title {:en "Ornaments"
             :de "Prachtstücke"}
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p strings/alpha-feature]]]
     :buttons [{:icon "fas fa-plus"
                :title strings/add
                :menu [{:title strings/mantling
                        :handler #(state/dispatch-on-event % [:add-element elements-path
                                                              default/mantling-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title strings/compartment
                        :handler #(state/dispatch-on-event % [:add-element elements-path
                                                              default/compartment-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title strings/supporter-left
                        :handler #(state/dispatch-on-event % [:add-element elements-path
                                                              default/supporter-left-charge
                                                              shield-separator/add-element-options])}
                       {:title strings/supporter-right
                        :handler #(state/dispatch-on-event % [:add-element elements-path
                                                              default/supporter-right-charge
                                                              shield-separator/add-element-options])}
                       {:title strings/motto
                        :handler #(state/dispatch-on-event % [:add-element elements-path default/motto
                                                              shield-separator/add-element-options])}
                       {:title strings/slogan
                        :handler #(state/dispatch-on-event % [:add-element elements-path default/slogan
                                                              shield-separator/add-element-options])}
                       {:title strings/charge
                        :handler #(state/dispatch-on-event % [:add-element elements-path default/ornament-charge
                                                              shield-separator/add-element-options])}
                       {:title strings/charge-group
                        :handler #(state/dispatch-on-event % [:add-element elements-path default/ornament-charge-group
                                                              shield-separator/add-element-options])}]}]
     :nodes (->> (range num-elements)
                 reverse
                 (map (fn [idx]
                        (let [motto-path (conj elements-path idx)
                              removable? @(rf/subscribe [:element-removable? motto-path])]
                          {:path motto-path
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip strings/move-down
                                              :handler #(state/dispatch-on-event % [:move-element motto-path (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-elements))
                                              :tooltip strings/move-up
                                              :handler #(state/dispatch-on-event % [:move-element motto-path (inc idx)])}]
                                      removable? (conj {:icon "far fa-trash-alt"
                                                        :tooltip strings/remove
                                                        :handler #(state/dispatch-on-event
                                                                   %
                                                                   [:remove-element motto-path
                                                                    shield-separator/remove-element-options])}))}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/ornaments [_path]
  {:form form})
