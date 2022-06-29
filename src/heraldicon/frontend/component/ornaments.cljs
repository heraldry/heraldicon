(ns heraldicon.frontend.component.ornaments
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.state :as state]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defmethod ui.interface/component-node-data :heraldry/ornaments [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title :string.charge.attribute.group/ornaments
     :selectable? false
     :buttons [{:icon "fas fa-plus"
                :title :string.button/add
                :menu [{:title :string.charge.attribute/mantling
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/mantling-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title :string.charge.attribute/compartment
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/compartment-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title :string.charge.attribute/supporter-left
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/supporter-left-charge
                                                              shield-separator/add-element-options])}
                       {:title :string.charge.attribute/supporter-right
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/supporter-right-charge
                                                              shield-separator/add-element-options])}
                       {:title :string.entity/motto
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/motto
                                                              shield-separator/add-element-options])}
                       {:title :string.entity/slogan
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/slogan
                                                              shield-separator/add-element-options])}
                       {:title :string.entity/charge
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/ornament-charge
                                                              shield-separator/add-element-options])}
                       {:title :string.entity/charge-group
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/ornament-charge-group
                                                              shield-separator/add-element-options])}]}]
     :nodes (->> (range num-elements)
                 reverse
                 (map (fn [idx]
                        (let [ornament-context (c/++ elements-context idx)
                              removable? @(rf/subscribe [:element-removable? ornament-context])]
                          {:context ornament-context
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :title :string.tooltip/move-down
                                              :handler #(state/dispatch-on-event % [:move-element ornament-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-elements))
                                              :title :string.tooltip/move-up
                                              :handler #(state/dispatch-on-event % [:move-element ornament-context (inc idx)])}]
                                      removable? (conj {:icon "far fa-trash-alt"
                                                        :remove? true
                                                        :title :string.tooltip/remove
                                                        :handler #(state/dispatch-on-event
                                                                   %
                                                                   [:remove-element ornament-context
                                                                    shield-separator/remove-element-options])}))}))))}))
