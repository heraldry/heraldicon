(ns heraldry.frontend.ui.element.fimbriation
  (:require [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]))

(defn fimbriation-submenu [path options & {:keys [label]}]
  (let [representation "fimbriation"]
    [:div.ui-setting
     (when label
       [:label label])
     [:div.option
      [submenu/submenu path label representation {:width "30em"}
       (for [option [:mode
                     :alignment
                     :corner
                     :thickness-1
                     :tincture-1
                     :thickness-2
                     :tincture-2]]
         ^{:key option} [interface/form-element (conj path option) (get options option)])]]]))

(defmethod interface/form-element :fimbriation [path {:keys [ui] :as options}]
  (when options
    [fimbriation-submenu
     path
     options
     :label (:label ui)]))
