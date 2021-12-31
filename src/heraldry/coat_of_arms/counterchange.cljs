(ns heraldry.coat-of-arms.counterchange
  (:require
   [re-frame.core :as rf]))

(defn collect-tinctures [field {:keys [tincture-mapping]}]
  (->> field
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) (fn [data]
                                  (if (map? data)
                                    ;; look in this order to really find the most important two tinctures
                                    (concat (-> data :fields)
                                            [(-> data :field)]
                                            [(-> data :charge)]
                                            (-> data :components)
                                            (-> data :charges))
                                    (seq data))))

       (filter #(and (map? %)
                     (-> % :type (= :heraldry.field.type/plain))
                     (-> % :tincture)))
       (map :tincture)
       (map #(get tincture-mapping % %))
       distinct))

(defn get-counterchange-tinctures [data context]
  (-> data
      (collect-tinctures context)
      (->> (take 2))))

(rf/reg-sub :get-counterchange-tinctures
  (fn [[_ path _context] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path context]]
    (get-counterchange-tinctures data context)))
