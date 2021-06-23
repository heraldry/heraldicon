(ns heraldry.frontend.ui.element.line
  (:require [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]))

(defn line-submenu [path options & {:keys [label]}]
  (let [representation "line"]
    [:div.ui-setting
     (when label
       [:label label])
     [:div.option
      [submenu/submenu path label representation {:width "30em"}
       (for [option [:type
                     :eccentricity
                     :height
                     :width
                     :spacing
                     :offset
                     :base-line
                     :mirrored?
                     :flipped?]]
         ^{:key option} [interface/form-element (conj path option) (get options option)])]]]))

(defmethod interface/form-element :line [path {:keys [ui] :as options}]
  (when options
    [line-submenu
     path
     options
     :label (:label ui)]))
