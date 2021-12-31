(ns heraldry.frontend.ui.form.collection
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]))

(defn form [context]
  [ui-interface/form-element (c/++ context :num-columns)])

(defmethod ui-interface/component-node-data :heraldry.component/collection [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    {:title :string.entity/arms
     :buttons [{:icon "fas fa-plus"
                :title :string.button/add
                :menu [{:title :string.entity/arms
                        :handler #(state/dispatch-on-event % [:add-element elements-context {}])}]}]
     :nodes (->> (range num-elements)
                 (map (fn [idx]
                        (let [element-context (c/++ elements-context idx)]
                          {:context element-context
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :tooltip :string.tooltip/move-down
                                      :handler #(state/dispatch-on-event % [:move-element element-context (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-elements))
                                      :tooltip :string.tooltip/move-up
                                      :handler #(state/dispatch-on-event % [:move-element element-context (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :remove? true
                                      :tooltip :string.tooltip/remove
                                      :handler #(state/dispatch-on-event % [:remove-element element-context])}]})))
                 vec)}))

(defmethod ui-interface/component-form-data :heraldry.component/collection [_context]
  {:form form})
