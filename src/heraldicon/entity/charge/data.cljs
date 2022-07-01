(ns heraldicon.entity.charge.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(derive :heraldicon.entity.charge/data :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldicon.entity.charge/data [_context]
  #{[:landscape?]})

(defmethod interface/options :heraldicon.entity.charge/data [context]
  (cond-> {:charge-type {:type :text
                         :ui {:label :string.option/charge-type
                              :tooltip :string.tooltip/charge-type}}
           :attributes {:ui {:form-type :ui.element/attributes}}
           :landscape? {:type :boolean
                        :ui {:label :string.option/landscape?
                             :tooltip :string.tooltip/landscape?}}}
    (not (interface/get-raw-data (c/++ context :landscape?)))
    (merge {:attitude {:type :choice
                       :choices attributes/attitude-choices
                       :default :none
                       :ui {:label :string.option/attitude}}
            :facing {:type :choice
                     :choices attributes/facing-choices
                     :default :none
                     :ui {:label :string.option/facing}}
            :colours {:ui {:form-type :ui.element/colours}}
            :fixed-tincture {:type :choice
                             :choices tincture/fixed-tincture-choices
                             :default :none
                             :ui {:label :string.option/fixed-tincture
                                  :tooltip :string.tooltip/fixed-tincture}}})))
