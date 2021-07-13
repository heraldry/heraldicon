(ns heraldry.frontend.ui.form.arms-general
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.license :as license]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :tags]]
     ^{:key option} [interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]])

(defmethod interface/component-node-data :heraldry.component/arms-general [_path _component-data _component-options]
  {:title "General"})

(defmethod interface/component-form-data :heraldry.component/arms-general [_path _component-data _component-options]
  {:form form})

;; TODO: might not be the right place for it, others live in the coat-of-arms.[thing].options namespaces
(defmethod interface/component-options :arms-general [_data _path]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution license/default-options
   :tags {:ui {:form-type :tags}}})
