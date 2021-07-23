(ns heraldry.frontend.ui.form.field
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.tincture-select :as tincture-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-event-db :override-field-part-reference
  (fn [db [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      (-> db
          (assoc-in path referenced-part)
          (state/ui-component-node-select path :open? true)))))

(rf/reg-event-fx :reset-field-part-reference
  (fn [{:keys [db]} [_ path]]
    (let [index (last path)
          parent (get-in db (drop-last 2 path))]
      {:db (assoc-in db path (-> (field/default-fields parent)
                                 (get index)))})))

(defn show-tinctures-only? [field-type]
  (-> field-type name keyword
      #{:chequy
        :lozengy
        :vairy
        :potenty
        :masonry
        :papellony}))

(defn form [path _]
  [:<>
   (for [option [:counterchanged?
                 :inherit-environment?
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
                 :outline?
                 :manual-blazon]]
     ^{:key option} [interface/form-element (conj path option)])
   (when (show-tinctures-only?
          @(rf/subscribe [:get-value (conj path :type)]))
     [:<>
      [:div {:style {:margin-bottom "1em"}}]
      (for [idx (range @(rf/subscribe [:get-list-size (conj path :fields)]))]
        ^{:key idx}
        [tincture-select/tincture-select (conj path :fields idx :tincture)])])])

(defn parent-path [path]
  (let [index (last path)
        parent-path (->> path (drop-last 2) vec)
        parent-type @(rf/subscribe [:get-value (conj parent-path :type)])]
    (when (and (int? index)
               (-> parent-type (or :dummy) namespace (= "heraldry.field.type")))
      parent-path)))

(defn name-prefix-for-part [path]
  (when-let [parent-path (parent-path path)]
    (let [parent-type @(rf/subscribe [:get-value (conj parent-path :type)])]
      (-> (field/part-name parent-type (last path))
          util/upper-case-first))))

(defn non-mandatory-part-of-parent? [path]
  (let [index (last path)]
    (when (int? index)
      (when-let [parent-path (parent-path path)]
        (>= index (field/mandatory-part-count parent-path))))))

(defmethod interface/component-node-data :heraldry.component/field [path]
  (let [field-type @(rf/subscribe [:get-value (conj path :type)])
        ref? (= field-type :heraldry.field.type/ref)
        components-path (conj path :components)
        num-components @(rf/subscribe [:get-list-size components-path])]
    {:title (util/combine ": "
                          [(name-prefix-for-part path)
                           (if ref?
                             (str "like " (name-prefix-for-part (-> path
                                                                    drop-last
                                                                    vec
                                                                    (conj
                                                                     @(rf/subscribe [:get-value (conj path :index)])))))
                             (field/title path))])
     :buttons (if ref?
                [{:icon "fas fa-sliders-h"
                  :title "Change"
                  :handler #(state/dispatch-on-event % [:override-field-part-reference path])}]
                (cond-> [{:icon "fas fa-plus"
                          :title "Add"
                          :menu [{:title "Ordinary"
                                  :handler #(state/dispatch-on-event % [:add-element components-path default/ordinary])}
                                 {:title "Charge"
                                  :handler #(state/dispatch-on-event % [:add-element components-path default/charge])}
                                 {:title "Charge group"
                                  :handler #(state/dispatch-on-event % [:add-element components-path default/charge-group])}
                                 {:title "Semy"
                                  :handler #(state/dispatch-on-event % [:add-element components-path default/semy])}]}]
                  (non-mandatory-part-of-parent? path)
                  (conj {:icon "fas fa-undo"
                         :title "Reset"
                         :handler #(state/dispatch-on-event % [:reset-field-part-reference path])})))
     :nodes (concat (when (and (not (show-tinctures-only? field-type))
                               (-> field-type name keyword (not= :plain)))
                      (->> (range @(rf/subscribe [:get-list-size (conj path :fields)]))
                           (map (fn [idx]
                                  {:path (conj path :fields idx)}))
                           vec))
                    (->> (range num-components)
                         reverse
                         (map (fn [idx]
                                (let [component-path (conj components-path idx)]
                                  {:path component-path
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip "move down"
                                              :handler #(state/dispatch-on-event % [:move-element component-path (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :tooltip "move up"
                                              :handler #(state/dispatch-on-event % [:move-element component-path (inc idx)])}
                                             {:icon "far fa-trash-alt"
                                              :tooltip "remove"
                                              :handler #(state/dispatch-on-event % [:remove-element component-path])}]})))
                         vec))}))

(defmethod interface/component-form-data :heraldry.component/field [_path]
  {:form form})
