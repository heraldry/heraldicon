(ns heraldry.frontend.ui.form.collection
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(defn form [context]
  [ui-interface/form-element (c/++ context :num-columns)])

(defmethod ui-interface/component-node-data :heraldry.component/collection [{:keys [path] :as context}]
  (let [num-elements (interface/get-list-size (c/++ context :elements))]
    {:title strings/arms
     :buttons [{:icon "fas fa-plus"
                :title strings/add
                :menu [{:title strings/arms
                        :handler #(state/dispatch-on-event % [:add-element (conj path :elements) {}])}]}]
     :nodes (->> (range num-elements)
                 (map (fn [idx]
                        (let [component-path (conj path :elements idx)]
                          {:context (c/<< context :path component-path)
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :tooltip strings/move-down
                                      :handler #(state/dispatch-on-event % [:move-element component-path (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-elements))
                                      :tooltip strings/move-up
                                      :handler #(state/dispatch-on-event % [:move-element component-path (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :tooltip strings/remove
                                      :handler #(state/dispatch-on-event % [:remove-element component-path])}]})))
                 vec)}))

(defmethod ui-interface/component-form-data :heraldry.component/collection [_context]
  {:form form})
