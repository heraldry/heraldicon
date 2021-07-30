(ns heraldry.frontend.ui.form.collection-general
  (:require [heraldry.frontend.ui.interface :as ui-interface]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :tags]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]

   (for [option [:font]]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/collection-general [path]
  {:title "General"
   :validation @(rf/subscribe [:validate-collection-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/collection-general [_path]
  {:form form})
