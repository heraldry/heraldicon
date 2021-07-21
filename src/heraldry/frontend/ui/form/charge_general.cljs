(ns heraldry.frontend.ui.form.charge-general
  (:require [heraldry.attribution :as attribution]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.ui.element.checkbox :as checkbox]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.interface :as interface]))

(defn form [path _]
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
     ^{:key option} [ui-interface/form-element (conj path option)])

   ;; TODO: not ideal, probably should move this at some point
   [checkbox/checkbox (conj [:example-coa :render-options :preview-original?])]])

(defmethod ui-interface/component-node-data :heraldry.component/charge-general [_path]
  {:title "General"})

(defmethod ui-interface/component-form-data :heraldry.component/charge-general [_path]
  {:form form})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/component-options :heraldry.options/charge-general [_path data]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution (attribution/options (:attribution data))
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
   :colours {:ui {:form-type :colours}}
   :attributes {:ui {:form-type :attributes}}
   :fixed-tincture {:type :choice
                    :choices tincture/fixed-tincture-choices
                    :default :none
                    :ui {:label "Fixed tincture"}}})
