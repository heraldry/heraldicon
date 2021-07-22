(ns heraldry.coat-of-arms.field.core
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.semy.core] ;; needed for defmethods
            [heraldry.frontend.util :as frontend-util]
            [heraldry.options :as options]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn mandatory-part-count-old [{:keys [type] :as field}]
  (let [type (-> type name keyword)
        {:keys [layout]} (options/sanitize field (field-options/options field))]
    (if (:num-base-fields layout)
      (:num-base-fields layout)
      (case type
        nil 0
        :tierced-per-pale 3
        :tierced-per-fess 3
        :tierced-per-pairle 3
        :tierced-per-pairle-reversed 3
        :tierced-per-pile 3
        2))))

(defn mandatory-part-count [path]
  (let [field-type (-> @(rf/subscribe [:get-value (conj path :type)])
                       name keyword)
        num-base-fields @(rf/subscribe [:get-sanitized-value (conj path :layout :num-base-fields)])]
    (if num-base-fields
      num-base-fields
      (case field-type
        nil 0
        :tierced-per-pale 3
        :tierced-per-fess 3
        :tierced-per-pairle 3
        :tierced-per-pairle-reversed 3
        :tierced-per-pile 3
        2))))

(defn default-fields [{:keys [type] :as field}]
  (let [type (-> type name keyword)
        {:keys [layout]} (options/sanitize field (field-options/options field))
        {:keys [num-fields-x
                num-fields-y
                num-base-fields]} layout
        defaults [default/field
                  (-> default/field
                      (assoc :tincture :azure))
                  (-> default/field
                      (assoc :tincture :sable))
                  (-> default/field
                      (assoc :tincture :gules))
                  (-> default/field
                      (assoc :tincture :or))
                  (-> default/field
                      (assoc :tincture :vert))]]
    (cond
      (= :per-saltire type) (-> (subvec defaults 0 2)
                                (into [{:type :heraldry.field.type/ref
                                        :index 1}
                                       {:type :heraldry.field.type/ref
                                        :index 0}]))
      (= :quartered type) (-> (subvec defaults 0 2)
                              (into [{:type :heraldry.field.type/ref
                                      :index 1}
                                     {:type :heraldry.field.type/ref
                                      :index 0}]))
      (= :quarterly type) (-> (subvec defaults 0 2)
                              (into (map (fn [i]
                                           (nth defaults (mod (+ i 2) (count defaults))))
                                         (range (- num-base-fields 2))))
                              (into (->> (for [j (range num-fields-y)
                                               i (range num-fields-x)]
                                           {:type :heraldry.field.type/ref
                                            :index (mod (+ i j) num-base-fields)})
                                         (drop num-base-fields))))
      (= :gyronny type) (-> (subvec defaults 0 2)
                            (into [{:type :heraldry.field.type/ref
                                    :index 1}
                                   {:type :heraldry.field.type/ref
                                    :index 0}
                                   {:type :heraldry.field.type/ref
                                    :index 0}
                                   {:type :heraldry.field.type/ref
                                    :index 1}
                                   {:type :heraldry.field.type/ref
                                    :index 1}
                                   {:type :heraldry.field.type/ref
                                    :index 0}]))
      (= :paly type) (if (= num-fields-y 1)
                       (subvec defaults 0 1)
                       (-> (subvec defaults 0 2)
                           (into (map (fn [i]
                                        (nth defaults (mod (+ i 2) (count defaults))))
                                      (range (- num-base-fields 2))))
                           (into (map (fn [i]
                                        {:type :heraldry.field.type/ref
                                         :index (mod i num-base-fields)})
                                      (range (- num-fields-x num-base-fields))))))
      (= :barry type) (if (= num-fields-y 1)
                        (subvec defaults 0 1)
                        (-> (subvec defaults 0 2)
                            (into (map (fn [i]
                                         (nth defaults (mod (+ i 2) (count defaults))))
                                       (range (- num-base-fields 2))))
                            (into (map (fn [i]
                                         {:type :heraldry.field.type/ref
                                          :index (mod i num-base-fields)})
                                       (range (- num-fields-y num-base-fields))))))
      (= :chequy type) (if (= [num-fields-x num-fields-y] [1 1])
                         (subvec defaults 0 1)
                         (-> (subvec defaults 0 2)
                             (into (map (fn [i]
                                          (nth defaults (mod (+ i 2) (count defaults))))
                                        (range (- num-base-fields 2))))))
      (#{:vairy
         :potenty
         :papellony
         :masonry} type) [(-> default/field
                              (assoc :tincture :azure))
                          (-> default/field
                              (assoc :tincture :argent))]
      (#{:bendy
         :bendy-sinister} type) (-> (subvec defaults 0 2)
                                    (into (map (fn [i]
                                                 (nth defaults (mod (+ i 2) (count defaults))))
                                               (range (- num-base-fields 2))))
                                    (into (map (fn [i]
                                                 {:type :heraldry.field.type/ref
                                                  :index (mod i num-base-fields)})
                                               (range (- num-fields-y num-base-fields)))))
      (#{:tierced-per-pale
         :tierced-per-fess
         :tierced-per-pairle
         :tierced-per-pairle-reversed
         :per-pile} type) (into (subvec defaults 0 2)
                                [(nth defaults 2)])
      :else (subvec defaults 0 2))))

(defn part-name [field-type index]
  (-> field-type interface/part-names (get index) (or (util/to-roman (inc index)))))

(defn title [path]
  (if @(rf/subscribe [:get-sanitized-value (conj path :counterchanged?)])
    "Counterchanged field"
    (let [field-type @(rf/subscribe [:get-sanitized-value (conj path :type)])]
      (str
       (if (= field-type :heraldry.field.type/plain)
         (-> @(rf/subscribe [:get-value (conj path :tincture)])
             frontend-util/translate-tincture
             frontend-util/upper-case-first)
         (get field-options/field-map field-type))
       " field"))))
