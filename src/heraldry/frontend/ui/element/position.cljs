(ns heraldry.frontend.ui.element.position
  (:require [heraldry.coat-of-arms.position :as position]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :position-title
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [position [_ _path]]
    ;; TODO: smarter way is necessary, also getting the options, which relies on the parent
    (util/combine ", "
                  [(if-let [point (:point position)]
                     (position/anchor-point-map point)
                     "Default")
                   (when (or (-> position :offset-x (or 0) zero? not)
                             (-> position :offset-y (or 0) zero? not))
                     "adjusted")])))

(defn position-submenu [path options & {:keys [label]}]
  (let [title @(rf/subscribe [:position-title path])]
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
         ^{:key option} [interface/form-element (conj path option) (get options option)])]]]))

(defmethod interface/form-element :position [path _]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options]
      [position-submenu
       path
       options
       :label (:label ui)])))
