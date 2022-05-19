(ns heraldicon.frontend.ui.form.entity.collection.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]))

(defn form [context]
  (ui.interface/form-elements
   context
   [:num-columns
    :font]))

(defmethod ui.interface/component-node-data :heraldicon.entity.collection/data [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title :string.entity/arms
     :selectable? false
     :buttons [{:icon "fas fa-plus"
                :handler #(state/dispatch-on-event % [:add-element elements-context default/collection-element])}]
     :nodes (->> (range num-elements)
                 (map (fn [idx]
                        (let [element-context (c/++ elements-context idx)]
                          {:context element-context
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :title :string.tooltip/move-down
                                      :handler #(state/dispatch-on-event % [:move-element element-context (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-elements))
                                      :title :string.tooltip/move-up
                                      :handler #(state/dispatch-on-event % [:move-element element-context (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :remove? true
                                      :title :string.tooltip/remove
                                      :handler #(state/dispatch-on-event % [:remove-element element-context])}]})))
                 vec)}))

(defmethod ui.interface/component-form-data :heraldicon.entity.collection/data [_context]
  {:form form})
