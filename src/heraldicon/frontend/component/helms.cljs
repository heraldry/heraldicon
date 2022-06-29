(ns heraldicon.frontend.component.helms
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as element]
   [heraldicon.frontend.state :as state]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]))

(defmethod component/node :heraldry/helms [context]
  (let [elements-context (c/++ context :elements)
        num-helms (interface/get-list-size elements-context)]
    {:title :string.entity/helms-and-crests
     :selectable? false
     :buttons [{:icon "fas fa-plus"
                :handler #(state/dispatch-on-event % [::element/add elements-context default/helm])}]
     :nodes (->> (range num-helms)
                 (map (fn [idx]
                        (let [helm-context (c/++ elements-context idx)]
                          {:context helm-context
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :title :string.tooltip/move-down
                                      :handler #(state/dispatch-on-event % [::element/move helm-context (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-helms))
                                      :title :string.tooltip/move-up
                                      :handler #(state/dispatch-on-event % [::element/move helm-context (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :remove? true
                                      :title :string.tooltip/remove
                                      :handler #(state/dispatch-on-event % [::element/remove helm-context])}]}))))}))
