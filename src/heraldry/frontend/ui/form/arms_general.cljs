(ns heraldry.frontend.ui.form.arms-general
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(defn form [context]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :tags]]
     ^{:key option} [ui-interface/form-element (update context :path conj option)])

   [:div {:style {:height "1.5em"}}]])

(defmethod ui-interface/component-node-data :heraldry.component/arms-general [{:keys [path]}]
  {:title strings/general
   :validation @(rf/subscribe [:validate-arms-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/arms-general [_context]
  {:form form})
