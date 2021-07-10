(ns heraldry.frontend.ui.element.position
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :position-title
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[position options] [_ _path]]
    (let [sanitized-position (options/sanitize position options)
          changes (concat [(-> sanitized-position
                               :point
                               position/anchor-point-map)]
                          (when (some #(options/changed? % sanitized-position options)
                                      [:offset-x :offset-y :angle])
                            ["adjusted"])
                          (when (options/changed? :alignment sanitized-position options)
                            ["aligned"]))]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn position-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          title @(rf/subscribe [:position-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "35em"}
         (for [option [:point
                       :alignment
                       :angle
                       :offset-x
                       :offset-y
                       :type]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :position [path]
  [position-submenu path])
