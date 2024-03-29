(ns heraldicon.frontend.component.ornaments
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defmethod component/node :heraldry/ornaments [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title :string.charge.attribute.group/ornaments
     :selectable? false
     :drop-options-fn drag/drop-options
     :drop-fn drag/drop-fn
     :buttons [{:icon "fas fa-plus"
                :title :string.button/add
                :menu [{:title :string.charge.attribute/mantling
                        :handler #(rf/dispatch [::component.element/add elements-context
                                                default/mantling-charge
                                                shield-separator/add-element-insert-at-bottom-options])}
                       {:title :string.charge.attribute/compartment
                        :handler #(rf/dispatch [::component.element/add elements-context
                                                default/compartment-charge
                                                shield-separator/add-element-insert-at-bottom-options])}
                       {:title :string.charge.attribute/supporter-left
                        :handler #(rf/dispatch [::component.element/add elements-context
                                                default/supporter-left-charge])}
                       {:title :string.charge.attribute/supporter-right
                        :handler #(rf/dispatch [::component.element/add elements-context
                                                default/supporter-right-charge])}
                       {:title :string.entity/motto
                        :handler #(rf/dispatch [::component.element/add elements-context default/motto])}
                       {:title :string.entity/slogan
                        :handler #(rf/dispatch [::component.element/add elements-context default/slogan])}
                       {:title :string.entity/charge
                        :handler #(rf/dispatch [::component.element/add elements-context default/ornament-charge])}
                       {:title :string.entity/charge-group
                        :handler #(rf/dispatch [::component.element/add elements-context default/ornament-charge-group])}]}]
     :nodes (->> (range num-elements)
                 (map (fn [idx]
                        (let [ornament-context (c/++ elements-context idx)
                              removable? @(rf/subscribe [::component.element/removable? ornament-context])]
                          {:context ornament-context
                           :buttons (when removable?
                                      [{:icon "far fa-trash-alt"
                                        :remove? true
                                        :title :string.tooltip/remove
                                        :handler #(rf/dispatch [::component.element/remove ornament-context])}])}))))}))
