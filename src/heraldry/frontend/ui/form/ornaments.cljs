(ns heraldry.frontend.ui.form.ornaments
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.context :as c]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.shield-separator :as shield-separator]
   [re-frame.core :as rf]))

(defn form [_context]
  [:<>])

(defmethod ui-interface/component-node-data :heraldry.component/ornaments [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title (string "Ornaments")
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p (string "This feature is incomplete and likely going to change, so use with caution. :)")]]]
     :buttons [{:icon "fas fa-plus"
                :title (string "Add")
                :menu [{:title (string "Mantling")
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/mantling-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title (string "Compartment")
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/compartment-charge
                                                              shield-separator/add-element-insert-at-bottom-options])}
                       {:title (string "Supporter-left")
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/supporter-left-charge
                                                              shield-separator/add-element-options])}
                       {:title (string "Supporter-right")
                        :handler #(state/dispatch-on-event % [:add-element elements-context
                                                              default/supporter-right-charge
                                                              shield-separator/add-element-options])}
                       {:title (string "Motto")
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/motto
                                                              shield-separator/add-element-options])}
                       {:title (string "Slogan")
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/slogan
                                                              shield-separator/add-element-options])}
                       {:title (string "Charge")
                        :handler #(state/dispatch-on-event % [:add-element elements-context default/ornament-charge
                                                              shield-separator/add-element-options])}
                       {:title (string "Charge group")
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
                                              :tooltip (string "move down")
                                              :handler #(state/dispatch-on-event % [:move-element ornament-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-elements))
                                              :tooltip (string "move up")
                                              :handler #(state/dispatch-on-event % [:move-element ornament-context (inc idx)])}]
                                      removable? (conj {:icon "far fa-trash-alt"
                                                        :tooltip (string "remove")
                                                        :handler #(state/dispatch-on-event
                                                                   %
                                                                   [:remove-element ornament-context
                                                                    shield-separator/remove-element-options])}))}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/ornaments [_context]
  {:form form})
