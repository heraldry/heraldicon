(ns heraldry.shield-separator
  (:require [heraldry.coat-of-arms.default :as default]))

(defn shield-separator? [element]
  (-> element
      :type
      (= :heraldry.component/shield-separator)))

(def remove-element-options
  {:post-fn (fn [elements]
              (if (->> elements
                       (filter (comp not shield-separator?))
                       count
                       zero?)
                []
                elements))})

(def add-element-options
  {:post-fn (fn [elements]
              (if (-> elements count (= 1))
                (into [default/shield-separator]
                      elements)
                elements
                ))})

(def add-element-default-behind-options
  {:post-fn (fn [elements]
              (if (-> elements count (= 1))
                (into elements
                      [default/shield-separator])
                elements
                ))
   :selected-element-path-fn (fn [selected-path element _elements]
                               (if (shield-separator? element)
                                 (-> selected-path
                                     drop-last
                                     vec
                                     (conj 0))
                                 selected-path))})

(defn shield-separator-exists? [elements]
  (->> elements
       (filter shield-separator?)
       seq))

(defn element-indices-below-shield [elements]
  (if (shield-separator-exists? elements)
    (->> elements
         (map-indexed vector)
         (take-while (comp not shield-separator? second))
         (map first)
         vec)
    []))

(defn element-indices-above-shield [elements]
  (if (shield-separator-exists? elements)
    (->> elements
         (map-indexed vector)
         (drop-while (comp not shield-separator? second))
         (filter (comp not shield-separator? second))
         (map first)
         vec)
    (->> (count elements)
         range
         vec)))

(defn element-indices-with-position [elements]
  (vec (concat (map (fn [idx]
                      [idx true]) (element-indices-below-shield elements))
               (map (fn [idx]
                      [idx false]) (element-indices-above-shield elements)))))
