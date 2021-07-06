(ns heraldry.frontend.ui.form.field
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option [:inherit-environment?
                 :counterchanged?
                 :type
                 :tincture
                 :line
                 :opposite-line
                 :extra-line
                 :variant
                 :thickness
                 :origin
                 :direction-anchor
                 :anchor
                 :geometry
                 :layout
                 :outline?]]
     ^{:key option} [interface/form-element (conj path option)])])

(defn name-prefix-for-part [path]
  (let [index (last path)
        parent-field-type @(rf/subscribe [:get-value (-> (drop-last 2 path)
                                                         vec
                                                         (conj :type))])]
    (when (and (int? index)
               (-> parent-field-type (or :dummy) namespace (= "heraldry.field.type")))
      (-> (field/part-name parent-field-type index)
          util/upper-case-first))))

(defmethod interface/component-node-data :heraldry.component/field [path component-data]
  {:title (util/combine ": "
                        [(name-prefix-for-part path)
                         (field/title component-data)])
   :buttons [{:icon "fas fa-plus"
              :menu [{:title "Ordinary"
                      :handler #(state/dispatch-on-event % [:add-component path default/ordinary])}
                     {:title "Charge"
                      :handler #(state/dispatch-on-event % [:add-component path default/charge])}
                     {:title "Charge group"
                      :handler #(state/dispatch-on-event % [:add-component path default/charge-group])}
                     {:title "Semy"
                      :handler #(state/dispatch-on-event % [:add-component path default/semy])}]}]
   :nodes (concat (when (-> component-data :type name keyword (not= :plain))
                    (->> component-data
                         :fields
                         count
                         range
                         (map (fn [idx]
                                {:path (conj path :fields idx)}))
                         vec))
                  (->> component-data
                       :components
                       count
                       range
                       reverse
                       (map (fn [idx]
                              (let [component-path (conj path :components idx)]
                                {:path component-path
                                 :buttons [{:icon "fas fa-chevron-down"
                                            :disabled? (zero? idx)
                                            :tooltip "move down"
                                            :handler #(state/dispatch-on-event % [:move-element-down component-path])}
                                           {:icon "fas fa-chevron-up"
                                            :disabled? (= idx (dec (count (:components component-data))))
                                            :tooltip "move up"
                                            :handler #(state/dispatch-on-event % [:move-element-up component-path])}
                                           {:icon "far fa-trash-alt"
                                            :tooltip "remove"
                                            :handler #(state/dispatch-on-event
                                                       % [:remove-element component-path])}]})))
                       vec))})

(defmethod interface/component-form-data :heraldry.component/field [_component-data]
  {:form form})
