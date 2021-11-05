(ns heraldry.frontend.ui.form.ornaments
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.context :as c]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.shield-separator :as shield-separator]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(defn form [_context]
  [:<>])

(defmethod ui-interface/component-node-data :heraldry.component/ornaments [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
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
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/mantling-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title strings/compartment
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/compartment-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title strings/supporter-left
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/supporter-left-charge
                                                              shield-separator/add-element-options])}
                       {:title strings/supporter-right
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/supporter-right-charge
                                                              shield-separator/add-element-options])}
                       {:title strings/motto
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/motto
                                                              shield-separator/add-element-options])}
                       {:title strings/slogan
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/slogan
                                                              shield-separator/add-element-options])}
                       {:title strings/charge
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/ornament-charge
                                                              shield-separator/add-element-options])}
                       {:title strings/charge-group
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/ornament-charge-group
                                                              shield-separator/add-element-options])}]}]
     :nodes (->> (range num-elements)
                 reverse
                 (map (fn [idx]
                        (let [motto-context (c/++ elements-context idx)
                              removable? @(rf/subscribe [:element-removable? motto-context])]
                          {:context motto-context
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip strings/move-down
                                              :handler #(state/dispatch-on-event % [:move-element motto-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-elements))
                                              :tooltip strings/move-up
                                              :handler #(state/dispatch-on-event % [:move-element motto-context (inc idx)])}]
                                      removable? (conj {:icon "far fa-trash-alt"
                                                        :tooltip strings/remove
                                                        :handler #(state/dispatch-on-event
                                                                   %
                                                                   [:remove-element motto-context
                                                                    shield-separator/remove-element-options])}))}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/ornaments [_context]
  {:form form})
