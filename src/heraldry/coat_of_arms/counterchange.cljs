(ns heraldry.coat-of-arms.counterchange
  (:require [clojure.walk :as walk]))

(defn collect-tinctures [field]
  (->> field
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) (fn [data]
                                  (if (map? data)
                                    ;; look in this order to really find the most important two tinctures
                                    (concat (-> data :division :fields)
                                            [(-> data :field)]
                                            (-> data :components))
                                    (seq data))))

       (filter #(and (map? %)
                     (-> % :component (= :field))
                     (-> % :content :tincture)))
       (map (comp :tincture :content))
       distinct))

(defn field-up-to-component [component field]
  (update field :components (fn [components]
                              (->> components
                                   (take-while #(not= % component))))))

(defn get-counterchange-tinctures [field]
  (-> field
      collect-tinctures
      (->> (take 2))))

(defn counterchange-field [component parent]
  (let [parent-up-to-component (field-up-to-component component parent)
        [tincture-1 tincture-2] (get-counterchange-tinctures parent-up-to-component)
        tincture-map {tincture-1 tincture-2
                      tincture-2 tincture-1}]
    (if (and tincture-1 tincture-2)
      (-> parent-up-to-component
          (assoc :counterchanged? true)
          (->> (walk/postwalk #(cond-> %
                                 (and (vector? %)
                                      (-> % second tincture-map)) ((fn [[k v]]
                                                                     [k (tincture-map v)])))))
          (update :components concat (-> component :field :components)))
      (:field component))))
