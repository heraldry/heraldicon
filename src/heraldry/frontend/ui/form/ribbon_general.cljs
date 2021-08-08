(ns heraldry.frontend.ui.form.ribbon-general
  (:require [heraldry.frontend.ui.interface :as ui-interface]
            [re-frame.core :as rf]))

(rf/reg-event-db :ribbon-edit-annotate-segments
  (fn [db [_ path]]
    (let [points (get-in db path)]
      db)))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :attributes
                 :tags]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   (for [option [:thickness]]
     ^{:key option} [ui-interface/form-element (conj path :ribbon option)])

   [:<>
    [:button {:on-click #(rf/dispatch [:ribbon-edit-annotate-segments path])}
     "center defaults"]]])

(defmethod ui-interface/component-node-data :heraldry.component/ribbon-general [path]
  {:title "General"
   :validation @(rf/subscribe [:validate-ribbon-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/ribbon-general [_path]
  {:form form})

