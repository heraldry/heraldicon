(ns heraldry.frontend.ui.form.arms-general
  (:require [heraldry.frontend.ui.interface :as ui-interface]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :tags]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]])

(defmethod ui-interface/component-node-data :heraldry.component/arms-general [_path]
  {:title "General"})

(defmethod ui-interface/component-form-data :heraldry.component/arms-general [_path]
  {:form form})
