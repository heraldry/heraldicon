(ns heraldry.frontend.ui.element.geometry
  (:require [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(rf/reg-sub :geometry-title
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [geometry [_ _path]]
    ;; TODO: smarter way is necessary, also getting the options, which relies on the parent
    "Change"))

(defn geometry-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          title @(rf/subscribe [:geometry-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "30em"}
         (for [option [:width
                       :thickness
                       :size-mode
                       :size
                       :eccentricity
                       :stretch
                       :mirrored?
                       :reversed?]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :geometry [path]
  [geometry-submenu path])
