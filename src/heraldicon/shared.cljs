(ns heraldicon.shared
  (:require
   ["paper" :refer [paper Size]]
   [heraldicon.attribution :as attribution]
   [heraldicon.heraldry.attributes :as attributes]
   [heraldicon.heraldry.charge-group.core] ;; needed for side effects
   [heraldicon.heraldry.charge-group.options] ;; needed for side effects
   [heraldicon.heraldry.charge.core] ;; needed for side effects
   [heraldicon.heraldry.charge.options] ;; needed for side effects
   [heraldicon.heraldry.charge.other] ;; needed for side effects
   [heraldicon.heraldry.coat-of-arms] ;; needed for side effects
   [heraldicon.heraldry.field.core] ;; needed for side effects
   [heraldicon.heraldry.field.shared] ;; needed for side effects
   [heraldicon.heraldry.ordinary.core] ;; needed for side effects
   [heraldicon.heraldry.ordinary.options] ;; needed for side effects
   [heraldicon.heraldry.semy.core] ;; needed for side effects
   [heraldicon.heraldry.semy.options] ;; needed for side effects
   [heraldicon.heraldry.tincture.core :as tincture]
   [heraldicon.collection.element] ;; needed for side effects
   [heraldicon.collection.options] ;; needed for side effects
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.helm] ;; needed for side effects
   [heraldicon.interface :as interface]
   [heraldicon.metadata :as metadata]
   [heraldicon.motto] ;; needed for side effects
   [heraldicon.ornaments] ;; needed for side effects
   [heraldicon.render-options] ;; needed for side effects
   [heraldicon.ribbon :as ribbon]
   [heraldicon.state] ;; needed for side effects
   ))

(.setup paper (new Size 500 500))

(defmethod interface/options-subscriptions :heraldry.component/arms-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-arms.[thing].options namespaces
(defmethod interface/options :heraldry.component/arms-general [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :metadata (metadata/options (c/++ context :metadata))
   :tags {:ui {:form-type :tags}}})

(defmethod interface/options-subscriptions :heraldry.component/collection-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-collection.[thing].options namespaces
(defmethod interface/options :heraldry.component/collection-general [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :metadata (metadata/options (c/++ context :metadata))
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
                  :ui {:label :string.option/name}}
           :is-public {:type :boolean
                       :ui {:label :string.option/is-public}}
           :attribution (attribution/options (c/++ context :attribution))
           :metadata (metadata/options (c/++ context :metadata))
           :tags {:ui {:form-type :tags}}
           :type {:type :text
                  :ui {:label :string.option/charge-type}}
           :attributes {:ui {:form-type :attributes}}
           :landscape? {:type :boolean
                        :ui {:label :string.option/landscape?
                             :tooltip "Keep the SVG as-is, embedded graphics also are allowed. This is only a good idea if you want to use images as landscape backgrounds."}}}
    (not (interface/get-raw-data (c/++ context :landscape?)))
    (merge {:attitude {:type :choice
                       :choices attributes/attitude-choices
                       :default :none
                       :ui {:label :string.option/attitude}}
            :facing {:type :choice
                     :choices attributes/facing-choices
                     :default :none
                     :ui {:label :string.option/facing}}
            :colours {:ui {:form-type :colours}}
            :fixed-tincture {:type :choice
                             :choices tincture/fixed-tincture-choices
                             :default :none
                             :ui {:label :string.option/fixed-tincture}}})))

(defmethod interface/options-subscriptions :heraldry.component/ribbon-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/options :heraldry.component/ribbon-general [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :ribbon (ribbon/options context)
   :tags {:ui {:form-type :tags}}})
