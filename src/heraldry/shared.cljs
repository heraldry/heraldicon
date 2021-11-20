(ns heraldry.shared
  (:require
   ["paper" :refer [paper Size]]
   [heraldry.attribution :as attribution]
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.coat-of-arms.charge-group.core] ;; needed for side effects
   [heraldry.coat-of-arms.charge-group.options] ;; needed for side effects
   [heraldry.coat-of-arms.charge.core] ;; needed for side effects
   [heraldry.coat-of-arms.charge.options] ;; needed for side effects
   [heraldry.coat-of-arms.charge.other] ;; needed for side effects
   [heraldry.coat-of-arms.core] ;; needed for side effects
   [heraldry.coat-of-arms.field.core] ;; needed for side effects
   [heraldry.coat-of-arms.field.shared] ;; needed for side effects
   [heraldry.coat-of-arms.ordinary.core] ;; needed for side effects
   [heraldry.coat-of-arms.ordinary.options] ;; needed for side effects
   [heraldry.coat-of-arms.semy.core] ;; needed for side effects
   [heraldry.coat-of-arms.semy.options] ;; needed for side effects
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.collection.element] ;; needed for side effects
   [heraldry.collection.options] ;; needed for side effects
   [heraldry.context :as c]
   [heraldry.font :as font]
   [heraldry.helm] ;; needed for side effects
   [heraldry.interface :as interface]
   [heraldry.motto] ;; needed for side effects
   [heraldry.ornaments] ;; needed for side effects
   [heraldry.render-options] ;; needed for side effects
   [heraldry.ribbon :as ribbon]
   [heraldry.state] ;; needed for side effects
   [heraldry.strings :as strings]))

(.setup paper (new Size 500 500))

(defmethod interface/options-subscriptions :heraldry.component/arms-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-arms.[thing].options namespaces
(defmethod interface/options :heraldry.component/arms-general [context]
  {:name {:type :text
          :default ""
          :ui {:label strings/name}}
   :is-public {:type :boolean
               :ui {:label strings/make-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :tags {:ui {:form-type :tags}}})

(defmethod interface/options-subscriptions :heraldry.component/collection-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-collection.[thing].options namespaces
(defmethod interface/options :heraldry.component/collection-general [context]
  {:name {:type :text
          :default ""
          :ui {:label strings/name}}
   :is-public {:type :boolean
               :ui {:label strings/make-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :tags {:ui {:form-type :tags}}
   :font font/default-options})

(defmethod interface/options-subscriptions :heraldry.component/charge-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]
    [:landscape?]})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/options :heraldry.component/charge-general [context]
  (cond-> {:name {:type :text
                  :default ""
                  :ui {:label strings/name}}
           :is-public {:type :boolean
                       :ui {:label strings/make-public}}
           :attribution (attribution/options (c/++ context :attribution))
           :tags {:ui {:form-type :tags}}
           :type {:type :text
                  :ui {:label {:en "Charge type"
                               :de "Wappenfigurtyp"}}}
           :attributes {:ui {:form-type :attributes}}
           :landscape? {:type :boolean
                        :ui {:label {:en "Landscape"
                                     :de "Landschaft"}
                             :tooltip "Keep the SVG as-is, embedded graphics also are allowed. This is only a good idea if you want to use images as landscape backgrounds."}}}
    (not (interface/get-raw-data (c/++ context :landscape?)))
    (merge {:attitude {:type :choice
                       :choices attributes/attitude-choices
                       :default :none
                       :ui {:label {:en "Attitude"
                                    :de "Haltung"}}}
            :facing {:type :choice
                     :choices attributes/facing-choices
                     :default :none
                     :ui {:label {:en "Facing"
                                  :de "Blickrichtung"}}}
            :colours {:ui {:form-type :colours}}
            :fixed-tincture {:type :choice
                             :choices tincture/fixed-tincture-choices
                             :default :none
                             :ui {:label {:en "Fixed tincture"
                                          :de "Feste Tinktur"}}}})))

(defmethod interface/options-subscriptions :heraldry.component/ribbon-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/options :heraldry.component/ribbon-general [context]
  {:name {:type :text
          :default ""
          :ui {:label strings/name}}
   :is-public {:type :boolean
               :ui {:label strings/make-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :ribbon (ribbon/options context)
   :tags {:ui {:form-type :tags}}})
