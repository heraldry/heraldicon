(ns heraldicon.entity.charge
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.metadata :as metadata]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

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
