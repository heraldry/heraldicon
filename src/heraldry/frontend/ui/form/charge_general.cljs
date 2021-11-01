(ns heraldry.frontend.ui.form.charge-general
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.element.checkbox :as checkbox]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(defn form [context]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :type
                 :attitude
                 :facing
                 :colours
                 :fixed-tincture
                 :attributes
                 :tags]]
     ^{:key option} [ui-interface/form-element (c/++ context option)])

   ;; TODO: not ideal, probably should move this at some point
   [checkbox/checkbox (conj [:example-coa :render-options :preview-original?])]])

(defmethod ui-interface/component-node-data :heraldry.component/charge-general [{:keys [path]}]
  {:title strings/general
   :validation @(rf/subscribe [:validate-charge-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/charge-general [_context]
  {:form form})
