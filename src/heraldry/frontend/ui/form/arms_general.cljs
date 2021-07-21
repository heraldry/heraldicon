(ns heraldry.frontend.ui.form.arms-general
  (:require [heraldry.attribution :as attribution]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.interface :as interface]))

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

;; TODO: might not be the right place for it, others live in the coat-of-arms.[thing].options namespaces
(defmethod interface/component-options :heraldry.options/arms-general [_path data]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution (attribution/options (:attribution data))
   :tags {:ui {:form-type :tags}}})
