(ns heraldry.frontend.ui.element.field-layout
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :field-layout-title
  (fn [[_ path] _]
    (rf/subscribe [:get (drop-last path)]))

  (fn [field [_ _path]]
    (let [stripped-field-type (-> field :type name keyword)
          effective-data (:layout (options/sanitize field (field-options/options field)))]
      (util/combine
       ", "
       [(cond
          (= stripped-field-type :paly) (str (:num-fields-x effective-data) " fields")
          (#{:barry
             :bendy
             :bendy-sinister} stripped-field-type) (str (:num-fields-y effective-data) " fields")
          (#{:quarterly
             :chequy
             :lozengy
             :vairy
             :potenty
             :papellony
             :masonry
             :bendy} stripped-field-type) (str (:num-fields-x effective-data) "x"
                                               (:num-fields-y effective-data) " fields"))
        (when (-> effective-data :num-base-fields (not= 2))
          (str (:num-base-fields effective-data) " base fields"))
        (when (or (-> effective-data :offset-x zero? not)
                  (-> effective-data :offset-y zero? not))
          (str "shifted"))
        (when (or (-> effective-data :stretch-x (not= 1))
                  (-> effective-data :stretch-y (not= 1)))
          (str "stretched"))
        (when (-> effective-data :rotation zero? not)
          (str "rotated"))]))))

(defn layout-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          title @(rf/subscribe [:field-layout-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "30em"}
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
