(ns heraldry.frontend.ui.form.collection-general
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(defn form [{:keys [path]}]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :tags]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]

   (for [option [:font]]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/collection-general [{:keys [path]}]
  {:title strings/general
   :validation @(rf/subscribe [:validate-collection-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/collection-general [_context]
  {:form form})
