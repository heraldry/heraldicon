(ns heraldry.frontend.ui.element.semy-layout
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.semy.options :as semy-options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :semy-layout-title
  (fn [[_ path] _]
    (rf/subscribe [:get (drop-last path)]))

  (fn [semy [_ _path]]
    (let [effective-data (:layout (options/sanitize semy semy-options/default-options))]
      (util/combine
       ", "
       [(str (:num-fields-x effective-data) "x"
             (:num-fields-y effective-data))
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
          title @(rf/subscribe [:semy-layout-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "30em"}
         (for [option [:num-fields-x
                       :num-fields-y
                       :offset-x
                       :offset-y
                       :stretch-x
                       :stretch-y
                       :rotation]]
           ^{:key option} [interface/form-element (conj path option) (get options option)])]]])))

(defmethod interface/form-element :semy-layout [path _]
  [layout-submenu path])
