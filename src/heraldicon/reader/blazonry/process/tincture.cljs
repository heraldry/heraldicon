(ns heraldicon.reader.blazonry.process.tincture
  (:require
   [clojure.walk :as walk]
   [heraldicon.reader.blazonry.transform.tincture :as tincture]))

(defn- last-tincture [tinctures tincture-same-reference]
  (->> tinctures
       (take-while #(not= % tincture-same-reference))
       (remove map?)
       last))

(defn process [hdn tinctures]
  (let [first-phase (walk/prewalk
                     (fn [data]
                       (if (map? data)
                         (condp apply [data]
                           ::tincture/tincture-ordinal-reference
                           :>> (fn [tincture-reference]
                                 (let [index (dec tincture-reference)]
                                   (if (and (<= 0 index)
                                            (< index (count tinctures)))
                                     (get tinctures index)
                                     :void)))

                           ::tincture/tincture-same-id
                           (or (last-tincture tinctures data)
                               :void)

                           data)
                         data))
                     hdn)
        root-field-without-components (select-keys
                                       (walk/prewalk
                                        (fn [data]
                                          (if (map? data)
                                            (if (::tincture/tincture-field-reference data)
                                              ;; if any of the subfields of the root field reference the field,
                                              ;; then that must be considered :void for this step, so we don't
                                              ;; get an infinite loop
                                              :void
                                              (dissoc data :components))
                                            data))
                                        first-phase)
                                       [:type :fields :tincture])]
    (walk/prewalk
     (fn [data]
       (if (map? data)
         (if (and (-> data :type (= :heraldry.field.type/plain))
                  (-> data :tincture ::tincture/tincture-field-reference))
           (-> data
               (dissoc :tincture)
               (merge root-field-without-components))
           (if (::tincture/tincture-field-reference data)
             :void
             data))
         data))
     first-phase)))
