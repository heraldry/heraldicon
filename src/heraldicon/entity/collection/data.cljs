(ns heraldicon.entity.collection.data
  (:require
   [heraldicon.font :as font]
   [heraldicon.interface :as interface]))

(derive :heraldicon.entity.collection/data :heraldry.options/root)

(defmethod interface/options :heraldicon.entity.collection/data [_context]
  {:num-columns {:type :option.type/range
                 :default 6
                 :min 1
                 :max 15
                 :ui/label :string.option/number-of-columns}
   :font font/default-options
   :font-title (assoc font/default-options :ui/label :string.option/font-title)})
