(ns heraldicon.frontend.component.helms
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defmethod component/node :heraldry/helms [context]
  (let [elements-context (c/++ context :elements)
        num-helms (interface/get-list-size elements-context)]
    {:title :string.entity/helms-and-crests
     :selectable? false
     :buttons [{:icon "fas fa-plus"
                :handler #(rf/dispatch [::component.element/add elements-context default/helm])}]
     :nodes (->> (range num-helms)
                 (map (fn [idx]
                        (let [helm-context (c/++ elements-context idx)]
                          {:context helm-context
                           :buttons [{:icon "far fa-trash-alt"
                                      :remove? true
                                      :title :string.tooltip/remove
                                      :handler #(rf/dispatch [::component.element/remove helm-context])}]}))))}))
