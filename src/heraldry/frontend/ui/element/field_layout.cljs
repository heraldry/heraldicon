(ns heraldry.frontend.ui.element.field-layout
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.field-type-select :as field-type-select]
   [heraldry.frontend.ui.element.range :as range]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :field-layout-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[layout options] [_ _path]]
    (let [sanitized-layout (options/sanitize layout options)
          main-name (when (or (:num-fields-x options)
                              (:num-fields-y options))
                      (util/str-tr (util/combine "x"
                                                 [(:num-fields-x sanitized-layout)
                                                  (:num-fields-y sanitized-layout)])
                                   {:en " fields"
                                    :de " Felder"}))
          changes (filter identity
                          [main-name
                           (when (options/changed? :num-base-fields sanitized-layout options)
                             (util/str-tr (:num-base-fields sanitized-layout) {:en " base fields"
                                                                               :de " Basisfelder"}))
                           (when (some #(options/changed? % sanitized-layout options)
                                       [:offset-x :offset-y])
                             strings/shifted)
                           (when (some #(options/changed? % sanitized-layout options)
                                       [:stretch-x :stretch-y])
                             strings/stretched)
                           (when (options/changed? :rotation sanitized-layout options)
                             strings/rotated)])
          changes (if (seq changes)
                    changes
                    [strings/default])]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(macros/reg-event-db :set-field-layout-num-fields-x
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       value
       (-> field :layout :num-fields-y)
       (-> field :layout :num-base-fields)))))

(macros/reg-event-db :set-field-layout-num-fields-y
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       (-> field :layout :num-fields-x)
       value
       (-> field :layout :num-base-fields)))))

(macros/reg-event-db :set-field-layout-num-base-fields
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       (-> field :layout :num-fields-x)
       (-> field :layout :num-fields-y)
       value))))

(defn layout-submenu [{:keys [path] :as context}]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:field-layout-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path label link-name {:style {:width "22em"}
                                               :class "submenu-field-layout"}
         (ui-interface/form-elements
          context
          [:num-base-fields
           :num-fields-x
           :num-fields-y
           :offset-x
           :offset-y
           :stretch-x
           :stretch-y
           :rotation])]]])))

(defmethod ui-interface/form-element :field-layout [context]
  [layout-submenu context])

(defmethod ui-interface/form-element :field-layout-num-fields-x [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-fields-x path value]))])

(defmethod ui-interface/form-element :field-layout-num-fields-y [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-fields-y path value]))])

(defmethod ui-interface/form-element :field-layout-num-base-fields [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-base-fields path value]))])
