(ns heraldry.frontend.ui.element.field-layout
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.field-type-select :as field-type-select]
   [heraldry.frontend.ui.element.range :as range]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(defn submenu-link-name [options layout]
  (let [main-name (when (or (:num-fields-x options)
                            (:num-fields-y options))
                    (util/str-tr (util/combine "x"
                                               [(:num-fields-x layout)
                                                (:num-fields-y layout)])
                                 :string.miscellaneous/fields))
        changes (filter identity
                        [main-name
                         (when (options/changed? :num-base-fields layout options)
                           (util/str-tr (:num-base-fields layout) " " :string.submenu-summary/base-fields))
                         (when (some #(options/changed? % layout options)
                                     [:offset-x :offset-y])
                           :string.submenu-summary/shifted)
                         (when (some #(options/changed? % layout options)
                                     [:stretch-x :stretch-y])
                           :string.submenu-summary/stretched)
                         (when (options/changed? :rotation layout options)
                           :string.submenu-summary/rotated)])
        changes (if (seq changes)
                  changes
                  [:string.submenu-summary/default])]
    (-> (util/combine ", " changes)
        util/upper-case-first)))

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

(defn layout-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
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
