(ns heraldicon.frontend.element.field-layout
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.field-type-select :as field-type-select]
   [heraldicon.frontend.element.range :as range]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(defn- submenu-link-name [options layout]
  (let [main-name (when (or (:num-fields-x options)
                            (:num-fields-y options))
                    (string/str-tr (string/combine "x"
                                                   [(:num-fields-x layout)
                                                    (:num-fields-y layout)])
                                   " "
                                   :string.miscellaneous/fields))
        changes (filter identity
                        [main-name
                         (when (options/changed? :num-base-fields layout options)
                           (string/str-tr (:num-base-fields layout) " " :string.submenu-summary/base-fields))
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
    (string/upper-case-first (string/combine ", " changes))))

(macros/reg-event-db ::set-num-fields-x
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       value
       (-> field :layout :num-fields-y)
       (-> field :layout :num-base-fields)
       (-> field :layout :base-field-shift)))))

(macros/reg-event-db ::set-num-fields-y
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       (-> field :layout :num-fields-x)
       value
       (-> field :layout :num-base-fields)
       (-> field :layout :base-field-shift)))))

(macros/reg-event-db ::num-base-fields
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       (-> field :layout :num-fields-x)
       (-> field :layout :num-fields-y)
       value
       (-> field :layout :base-field-shift)))))

(macros/reg-event-db ::base-field-shift
  (fn [db [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      (field-type-select/set-field-type
       db
       field-path
       (:type field)
       (-> field :layout :num-fields-x)
       (-> field :layout :num-fields-y)
       (-> field :layout :num-base-fields)
       value))))

(defmethod element/element :ui.element/field-layout [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-field-layout"}
         (element/elements
          context
          [:num-base-fields
           :base-field-shift
           :num-fields-x
           :num-fields-y
           :offset-x
           :offset-y
           :stretch-x
           :stretch-y
           :rotation])]]])))

(defmethod element/element :ui.element/field-layout-num-fields-x [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [::set-num-fields-x path value]))])

(defmethod element/element :ui.element/field-layout-num-fields-y [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [::set-num-fields-y path value]))])

(defmethod element/element :ui.element/field-layout-num-base-fields [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [::num-base-fields path value]))])

(defmethod element/element :ui.element/field-layout-base-field-shift [{:keys [path] :as context}]
  [range/range-input context
   :on-change (fn [value]
                (rf/dispatch [::base-field-shift path value]))])
