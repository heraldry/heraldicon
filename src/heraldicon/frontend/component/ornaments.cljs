(ns heraldicon.frontend.component.ornaments
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as element]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defmethod component/node :heraldry/ornaments [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title :string.charge.attribute.group/ornaments
     :selectable? false
     :buttons [{:icon "fas fa-plus"
                :title :string.button/add
                :menu [{:title :string.charge.attribute/mantling
                        :handler #(rf/dispatch [::element/add elements-context
                                                default/mantling-charge
                                                shield-separator/add-element-insert-at-bottom-options])}
                       {:title :string.charge.attribute/compartment
                        :handler #(rf/dispatch [::element/add elements-context
                                                default/compartment-charge
                                                shield-separator/add-element-insert-at-bottom-options])}
                       {:title :string.charge.attribute/supporter-left
                        :handler #(rf/dispatch [::element/add elements-context
                                                default/supporter-left-charge
                                                shield-separator/add-element-options])}
                       {:title :string.charge.attribute/supporter-right
                        :handler #(rf/dispatch [::element/add elements-context
                                                default/supporter-right-charge
                                                shield-separator/add-element-options])}
                       {:title :string.entity/motto
                        :handler #(rf/dispatch [::element/add elements-context default/motto
                                                shield-separator/add-element-options])}
                       {:title :string.entity/slogan
                        :handler #(rf/dispatch [::element/add elements-context default/slogan
                                                shield-separator/add-element-options])}
                       {:title :string.entity/charge
                        :handler #(rf/dispatch [::element/add elements-context default/ornament-charge
                                                shield-separator/add-element-options])}
                       {:title :string.entity/charge-group
                        :handler #(rf/dispatch [::element/add elements-context default/ornament-charge-group
                                                shield-separator/add-element-options])}]}]
     :nodes (->> (range num-elements)
                 reverse
                 (map (fn [idx]
                        (let [ornament-context (c/++ elements-context idx)
                              removable? @(rf/subscribe [::element/removable? ornament-context])]
                          {:context ornament-context
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :title :string.tooltip/move-down
                                              :handler #(rf/dispatch [::element/move ornament-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-elements))
                                              :title :string.tooltip/move-up
                                              :handler #(rf/dispatch [::element/move ornament-context (inc idx)])}]
                                      removable? (conj {:icon "far fa-trash-alt"
                                                        :remove? true
                                                        :title :string.tooltip/remove
                                                        :handler #(rf/dispatch [::element/remove ornament-context
                                                                                shield-separator/remove-element-options])}))}))))}))
