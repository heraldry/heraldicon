(ns heraldicon.frontend.ui.form.helms
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]))

(defmethod ui.interface/component-node-data :heraldry/helms [context]
  (let [elements-context (c/++ context :elements)
        num-helms (interface/get-list-size elements-context)]
    {:title :string.entity/helms-and-crests
     :selectable? false
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p [tr :string.tooltip/alpha-feature-warning]]]]
     :buttons [{:icon "fas fa-plus"
                :handler #(state/dispatch-on-event % [:add-element elements-context default/helm])}]
     :nodes (->> (range num-helms)
                 (map (fn [idx]
                        (let [helm-context (c/++ elements-context idx)]
                          {:context helm-context
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :title :string.tooltip/move-down
                                      :handler #(state/dispatch-on-event % [:move-element helm-context (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-helms))
                                      :title :string.tooltip/move-up
                                      :handler #(state/dispatch-on-event % [:move-element helm-context (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :remove? true
                                      :title :string.tooltip/remove
                                      :handler #(state/dispatch-on-event % [:remove-element helm-context])}]}))))}))
