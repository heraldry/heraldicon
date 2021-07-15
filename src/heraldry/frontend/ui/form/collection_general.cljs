(ns heraldry.frontend.ui.form.collection-general
  (:require [heraldry.font :as font]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.license :as license]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :tags]]
     ^{:key option} [interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]

   (for [option [:font]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/collection-general [_path _component-data _component-options]
  {:title "General"})

(defmethod interface/component-form-data :heraldry.component/collection-general [_path _component-data _component-options]
  {:form form})

;; TODO: might not be the right place for it, others live in the coat-of-collection.[thing].options namespaces
(defmethod interface/component-options :collection-general [_data _path]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution license/default-options
   :tags {:ui {:form-type :tags}}
   :font font/default-options})
