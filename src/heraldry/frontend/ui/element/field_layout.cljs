(ns heraldry.frontend.ui.element.field-layout
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :field-layout-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get (drop-last path)])
     (rf/subscribe [:get-relevant-options (drop-last path)])])

  (fn [[field options] [_ _path]]
    (let [stripped-field-type (-> field :type name keyword)
          sanitized-field (options/sanitize field options)
          sanitized-layout (:layout sanitized-field)
          layout-options (:layout options)
          main-name (cond
                      (#{:paly}
                       stripped-field-type) (str (:num-fields-x sanitized-layout) " fields")
                      (#{:barry
                         :bendy
                         :bendy-sinister}
                       stripped-field-type) (str (:num-fields-y sanitized-layout) " fields")
                      (#{:quarterly
                         :chequy
                         :lozengy
                         :vairy
                         :potenty
                         :papellony
                         :masonry
                         :bendy}
                       stripped-field-type) (str (:num-fields-x sanitized-layout) "x"
                                                 (:num-fields-y sanitized-layout) " fields"))
          changes [main-name
                   (when (options/changed? :num-base-fields sanitized-layout layout-options)
                     (str (:num-base-fields sanitized-layout) " base fields"))
                   (when (some #(options/changed? % sanitized-layout layout-options)
                               [:offset-x :offset-y])
                     "shifted")
                   (when (some #(options/changed? % sanitized-layout layout-options)
                               [:stretch-x :stretch-y])
                     "stretched")
                   (when (options/changed? :rotation sanitized-layout layout-options)
                     "rotated")]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

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
