(ns heraldicon.entity.charge.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(derive :heraldicon.entity.charge/data :heraldry.options/root)

(defmethod interface/options :heraldicon.entity.charge/data [context]
  (cond-> {:charge-type {:type :option.type/text
                         :ui/label :string.option/charge-type
                         :ui/tooltip :string.tooltip/charge-type
                         :ui/placeholder "lion, rose, staff, etc."
                         :ui/element :ui.element/charge-type-select}
           :attributes {:ui/element :ui.element/attributes}
           :landscape? {:type :option.type/boolean
                        :ui/label :string.option/landscape?
                        :ui/tooltip :string.tooltip/landscape?}}
    (not (interface/get-raw-data (c/++ context :landscape?)))
    (merge {:attitude {:type :option.type/choice
                       :choices attributes/attitude-choices
                       :default :none
                       :ui/label :string.option/attitude}
            :facing {:type :option.type/choice
                     :choices attributes/facing-choices
                     :default :none
                     :ui/label :string.option/facing}
            :colours {:ui/element :ui.element/colours}
            :fixed-tincture {:type :option.type/choice
                             :choices tincture/fixed-tincture-choices
                             :default :none
                             :ui/label :string.option/fixed-tincture
                             :ui/tooltip :string.tooltip/fixed-tincture}})))
