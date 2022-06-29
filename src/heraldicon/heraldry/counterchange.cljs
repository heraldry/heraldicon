(ns heraldicon.heraldry.counterchange
  (:require
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defn- collect-tinctures [field {:keys [tincture-mapping]}]
  (->> field
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) (fn [data]
                                  (if (map? data)
                                    ;; look in this order to find the most important two tinctures
                                    (concat (:fields data)
                                            [(:field data)]
                                            [(:charge data)]
                                            (:components data)
                                            (:charges data))
                                    (seq data))))

       (filter #(and (map? %)
                     (-> % :type (= :heraldry.field.type/plain))
                     (:tincture %)))
       (map :tincture)
       (map #(get tincture-mapping % %))
       distinct))

(defn- get-tinctures [data context]
  (-> data
      (collect-tinctures context)
      (->> (take 2))))

(defn tinctures [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (get-tinctures (interface/get-raw-data context) context)
    @(rf/subscribe [::tinctures path context])))

(rf/reg-sub ::tinctures
  (fn [[_ path _context] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path context]]
    (get-tinctures data context)))
