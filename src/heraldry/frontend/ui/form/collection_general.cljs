(ns heraldry.frontend.ui.form.collection-general
  (:require [heraldry.font :as font]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.interface :as interface]
            [heraldry.license :as license]))

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

(defmethod ui-interface/component-node-data :heraldry.component/collection-general [_path]
  {:title "General"})

(defmethod ui-interface/component-form-data :heraldry.component/collection-general [_path]
  {:form form})

;; TODO: might not be the right place for it, others live in the coat-of-collection.[thing].options namespaces
(defmethod interface/component-options :heraldry.options/collection-general [_path _data]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution license/default-options
   :tags {:ui {:form-type :tags}}
   :font font/default-options})
