(ns heraldry.frontend.ui.form.field
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.coat-of-arms.field.core :as field]
   [heraldry.context :as c]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.tincture-select :as tincture-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(macros/reg-event-db :override-field-part-reference
  (fn [db [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      (-> db
          (assoc-in path referenced-part)
          (state/ui-component-node-select path :open? true)))))

(macros/reg-event-db :reset-field-part-reference
  (fn [db [_ {:keys [path] :as context}]]
    (let [index (last path)
          parent-context (c/-- context 2)]
      (assoc-in db path (-> (field/default-fields parent-context)
                            (get index))))))

(defn show-tinctures-only? [field-type]
  (-> field-type name keyword
      #{:chequy
        :lozengy
        :vairy
        :potenty
        :masony
        :papellony
        :fretty}))

(defn form [context]
  [:<>
   (ui-interface/form-elements
    context
    [:inherit-environment?
     :type
     :tincture
     :line
     :opposite-line
     :extra-line
     :variant
     :thickness
     :gap
     :origin
     :direction-anchor
     :anchor
     :geometry
     :layout
     :outline?
     :manual-blazon])

   (when (show-tinctures-only? (interface/get-raw-data (c/++ context :type)))
     [:<>
      [:div {:style {:margin-bottom "1em"}}]
      (for [idx (range (interface/get-list-size (c/++ context :fields)))]
        ^{:key idx}
        [tincture-select/tincture-select (c/++ context :fields idx :tincture)])])])

(defn parent-context [{:keys [path] :as context}]
  (let [index (last path)
        parent-context (c/-- context 2)
        parent-type (interface/get-raw-data (c/++ parent-context :type))]
    (when (and (int? index)
               (-> parent-type (or :dummy) namespace (= "heraldry.field.type")))
      parent-context)))

(defn name-prefix-for-part [{:keys [path] :as context}]
  (when-let [parent-context (parent-context context)]
    (let [parent-type (interface/get-raw-data (c/++ parent-context :type))]
      (-> (field/part-name parent-type (last path))
          util/upper-case-first))))

(defn non-mandatory-part-of-parent? [{:keys [path] :as context}]
  (let [index (last path)]
    (when (int? index)
      (when-let [parent-context (parent-context context)]
        (>= index (field/mandatory-part-count parent-context))))))

(defmethod ui-interface/component-node-data :heraldry.component/field [{:keys [path] :as context}]
  (let [field-type (interface/get-raw-data (c/++ context :type))
        ref? (= field-type :heraldry.field.type/ref)
        components-context (c/++ context :components)
        num-components (interface/get-list-size components-context)]
    {:title (util/combine ": "
                          [(name-prefix-for-part context)
                           (if ref?
                             (str "like " (name-prefix-for-part
                                           (-> context
                                               c/--
                                               (c/++ (interface/get-raw-data
                                                      (c/++ context :index))))))
                             (field/title context))])
     :validation @(rf/subscribe [:validate-field path])
     :buttons (if ref?
                [{:icon "fas fa-sliders-h"
                  :title strings/change
                  :handler #(state/dispatch-on-event % [:override-field-part-reference path])}]
                (cond-> [{:icon "fas fa-plus"
                          :title strings/add
                          :menu [{:title strings/ordinary
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/ordinary])}
                                 {:title strings/charge
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/charge])}
                                 {:title strings/charge-group
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/charge-group])}
                                 {:title {:en "Semy"
                                          :de "Besähung"}
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/semy])}]}]
                  (non-mandatory-part-of-parent? context)
                  (conj {:icon "fas fa-undo"
                         :title "Reset"
                         :handler #(state/dispatch-on-event % [:reset-field-part-reference context])})))
     :nodes (concat (when (and (not (show-tinctures-only? field-type))
                               (-> field-type name keyword (not= :plain)))
                      (let [fields-context (c/++ context :fields)
                            num-fields (interface/get-list-size fields-context)]
                        (->> (range num-fields)
                             (map (fn [idx]
                                    {:context (c/++ fields-context idx)}))
                             vec)))
                    (->> (range num-components)
                         reverse
                         (map (fn [idx]
                                (let [component-context (c/++ components-context idx)]
                                  {:context component-context
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip strings/move-down
                                              :handler #(state/dispatch-on-event % [:move-element component-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :tooltip strings/move-up
                                              :handler #(state/dispatch-on-event % [:move-element component-context (inc idx)])}
                                             {:icon "far fa-trash-alt"
                                              :tooltip strings/remove
                                              :handler #(state/dispatch-on-event % [:remove-element component-context])}]})))
                         vec))}))

(defmethod ui-interface/component-form-data :heraldry.component/field [_context]
  {:form form})
