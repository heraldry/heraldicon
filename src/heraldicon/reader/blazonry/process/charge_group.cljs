(ns heraldicon.reader.blazonry.process.charge-group
  (:require
   [clojure.walk :as walk]
   [heraldicon.reader.blazonry.transform.charge-group :as charge-group]))

(defn- add-charge-group-defaults [{::charge-group/keys [default-charge-group-amount]
                                   :keys [type]
                                   :as hdn} & {:keys [parent-ordinary-type]}]
  (let [type-namespace (some-> type namespace)]
    (cond-> hdn
      default-charge-group-amount (->
                                    (dissoc ::charge-group/default-charge-group-amount)
                                    (merge
                                     (case (some-> parent-ordinary-type name keyword)
                                       :pale {:type :heraldry.charge-group.type/columns
                                              :spacing (/ 95 default-charge-group-amount)
                                              :strips [{:type :heraldry.charge-group.element.type/strip
                                                        :slots (vec (repeat default-charge-group-amount 0))}]}

                                       :chevron {:type :heraldry.charge-group.type/rows
                                                 :spacing (/ 90 default-charge-group-amount)
                                                 :strips (->> (range (-> default-charge-group-amount
                                                                         inc
                                                                         (/ 2)))
                                                              (map (fn [index]
                                                                     {:type :heraldry.charge-group.element.type/strip
                                                                      :stretch (if (and (zero? index)
                                                                                        (even? default-charge-group-amount))
                                                                                 1
                                                                                 (if (and (pos? index)
                                                                                          (even? default-charge-group-amount))
                                                                                   (+ 1 (/ (inc index)
                                                                                           index))
                                                                                   2))
                                                                      :slots (if (zero? index)
                                                                               (if (odd? default-charge-group-amount)
                                                                                 [0]
                                                                                 [0 0])
                                                                               (-> (concat [0]
                                                                                           (repeat (dec index) nil)
                                                                                           [0])
                                                                                   vec))}))
                                                              vec)}

                                       :bordure {:type :heraldry.charge-group.type/in-orle
                                                 :slots (vec (repeat default-charge-group-amount 0))}

                                       :orle {:type :heraldry.charge-group.type/in-orle
                                              :distance 2.5
                                              :slots (vec (repeat default-charge-group-amount 0))}

                                       {:type :heraldry.charge-group.type/rows
                                        :spacing (/ 95 default-charge-group-amount)
                                        :strips [{:type :heraldry.charge-group.element.type/strip
                                                  :slots (vec (repeat default-charge-group-amount 0))}]})))

      (= type-namespace
         "heraldry.ordinary.type") (update
                                    :field
                                    (fn [field]
                                      (cond-> field
                                        (:components field) (update
                                                             :components
                                                             (fn [components]
                                                               (mapv
                                                                (fn [component]
                                                                  (add-charge-group-defaults
                                                                   component
                                                                   :parent-ordinary-type type))
                                                                components)))))))))

(defn process [hdn]
  (walk/prewalk add-charge-group-defaults hdn))
