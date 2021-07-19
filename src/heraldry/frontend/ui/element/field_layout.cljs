(ns heraldry.frontend.ui.element.field-layout
  (:require [heraldry.options :as options]
            [heraldry.frontend.ui.element.field-type-select :as field-type-select]
            [heraldry.frontend.ui.element.range :as range]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :field-layout-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[layout options] [_ _path]]
    (let [sanitized-layout (options/sanitize layout options)
          main-name (str (util/combine "x"
                                       [(:num-fields-x sanitized-layout)
                                        (:num-fields-y sanitized-layout)])
                         " fields")
          changes [main-name
                   (when (options/changed? :num-base-fields sanitized-layout options)
                     (str (:num-base-fields sanitized-layout) " base fields"))
                   (when (some #(options/changed? % sanitized-layout options)
                               [:offset-x :offset-y])
                     "shifted")
                   (when (some #(options/changed? % sanitized-layout options)
                               [:stretch-x :stretch-y])
                     "stretched")
                   (when (options/changed? :rotation sanitized-layout options)
                     "rotated")]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(rf/reg-event-db :set-field-layout-num-fields-x
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

(rf/reg-event-db :set-field-layout-num-fields-y
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

(rf/reg-event-db :set-field-layout-num-base-fields
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

(defn layout-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:field-layout-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label link-name {:width "30em"}
         (for [option [:num-base-fields
                       :num-fields-x
                       :num-fields-y
                       :offset-x
                       :offset-y
                       :stretch-x
                       :stretch-y
                       :rotation]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :field-layout [path]
  [layout-submenu path])

(defmethod interface/form-element :field-layout-num-fields-x [path]
  [range/range-input path
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-fields-x path value]))])

(defmethod interface/form-element :field-layout-num-fields-y [path]
  [range/range-input path
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-fields-y path value]))])

(defmethod interface/form-element :field-layout-num-base-fields [path]
  [range/range-input path
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-base-fields path value]))])
