(ns heraldicon.reader.blazonry.process.ordinary
  (:require
   [clojure.walk :as walk]))

(defn- field? [data]
  (and (map? data)
       (-> data :type (isa? :heraldry/field))))

(defn- raise-chiefs-and-bases [data]
  (if (field? data)
    (update data :components (fn [components]
                               (vec (sort-by (fn [{:keys [type]}]
                                               (cond
                                                 (isa? type :heraldry/semy) 0
                                                 (#{:heraldry.ordinary.type/chief
                                                    :heraldry.ordinary.type/base} type) 2
                                                 (isa? type :heraldry/ordinary) 1
                                                 :else 3))
                                             < components))))
    data))

(defn process [hdn]
  (walk/postwalk raise-chiefs-and-bases hdn))
