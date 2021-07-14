(ns heraldry.frontend.ui.form.charge-general
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.license :as license]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.tincture.core :as tincture]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :type
                 :attitude
                 :facing
                 :attributes
                 :colours
                 :fixed-tincture
                 :tags]]
     ^{:key option} [interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]])

(defmethod interface/component-node-data :heraldry.component/charge-general [_path _component-data _component-options]
  {:title "General"})

(defmethod interface/component-form-data :heraldry.component/charge-general [_path _component-data _component-options]
  {:form form})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/component-options :charge-general [_data _path]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution license/default-options
   :tags {:ui {:form-type :tags}}
   :type {:type :text
          :ui {:label "Charge type"}}
   :attitude {:type :choice
              :choices attributes/attitude-choices
              :default :none
              :ui {:label "Attitude"}}
   :facing {:type :choice
            :choices attributes/facing-choices
            :default :none
            :ui {:label "Facing"}}
   :fixed-tincture {:type :choice
                    :choices tincture/fixed-tincture-choices
                    :default :none
                    :ui {:label "Fixed tincture"}}})
