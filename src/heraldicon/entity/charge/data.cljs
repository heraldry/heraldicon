(ns heraldicon.entity.charge.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(derive :heraldicon.charge/data :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldicon.charge/data [_context]
  #{[:landscape?]})

(defmethod interface/options :heraldicon.charge/data [context]
  (cond-> {:type {:type :text
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
