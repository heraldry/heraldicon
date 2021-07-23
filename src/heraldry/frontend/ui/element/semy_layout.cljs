(ns heraldry.frontend.ui.element.semy-layout
  (:require [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.options :as options]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :semy-layout-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[layout options] [_ _path]]
    (let [sanitized-layout (options/sanitize layout options)
          main-name (str (:num-fields-x sanitized-layout) "x"
                         (:num-fields-y sanitized-layout)
                         " fields")
          changes [main-name
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

(defn layout-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:semy-layout-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label link-name {:width "30em"}
         (for [option [:num-fields-x
                       :num-fields-y
                       :offset-x
                       :offset-y
                       :stretch-x
                       :stretch-y
                       :rotation]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :semy-layout [path]
  [layout-submenu path])
