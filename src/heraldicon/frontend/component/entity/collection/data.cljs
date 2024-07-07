(ns heraldicon.frontend.component.entity.collection.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defn- form [context]
  (element/elements
   context
   [:num-columns
    :font-title
    :font-scale-title
    :font
    :font-scale]))

(defmethod component/node :heraldicon.entity.collection/data [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title :string.entity/arms
     :selectable? false
     :buttons [{:icon "fas fa-plus"
                :handler #(rf/dispatch [::component.element/add elements-context default/collection-element])}]
     :nodes (->> (range num-elements)
                 (map (fn [idx]
                        (let [element-context (c/++ elements-context idx)]
                          {:context element-context
                           :buttons [{:icon "far fa-trash-alt"
                                      :remove? true
                                      :title :string.tooltip/remove
                                      :handler #(rf/dispatch [::component.element/remove element-context])}]})))
                 vec)}))

(defmethod component/form :heraldicon.entity.collection/data [_context]
  form)
