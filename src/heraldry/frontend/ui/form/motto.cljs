(ns heraldry.frontend.ui.form.motto
  (:require [heraldry.frontend.ui.interface :as ui-interface]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option [:type
                 :origin
                 :geometry
                 :ribbon-variant]]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/motto [path]
  (let [num-mottos @(rf/subscribe [:get-list-size (-> path drop-last vec)])
        title (case @(rf/subscribe [:get-value (conj path :type)])
                :heraldry.motto.type/motto "Motto"
                :heraldry.motto.type/slogan "Slogan")]
    {:title (str (when (> num-mottos 1)
                   (str (inc (last path)) ". ")) title)}))

(defmethod ui-interface/component-form-data :heraldry.component/motto [_path]
  {:form form})

