(ns heraldry.math.svg.core
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.util :as util]))

(defn split-style-value [value]
  (-> value
      (s/split #";")
      (->>
       (map (fn [chunk]
              (-> chunk
                  (s/split #":" 2)
                  (as-> [key value]
                    [(keyword (s/trim key)) (s/trim value)]))))
       (into {}))))

(defn fix-string-style-values [data]
  (walk/postwalk #(if (and (vector? %)
                           (-> % count (= 2))
                           (-> % first (= :style))
                           (-> % second string?))
                    [:style (split-style-value (second %))]
                    %)
                 data))

(defn fix-attribute-and-tag-names [data]
  (walk/postwalk
   (fn [v]
     (cond
       (keyword? v) (get {:pattern-units :patternUnits
                          :gradientunits :gradientUnits
                          :xlink:href :href
                          :gradienttransform :gradientTransform
                          :lineargradient :linearGradient
                          :radialgradient :radialGradient
                          :xml:space :xmlSpace} v v)
       :else v))
   data))

(defn replace-id-references [data id-map]
  (let [prepared-id-map (->> id-map
                             (map (fn [[k v]]
                                    [[k v]
                                     [(str "#" k) (str "#" v)]
                                     [(str "url(#" k ")") (str "url(#" v ")")]]))
                             (apply concat)
                             (into {}))]
    (walk/postwalk
     #(if (and (vector? %)
               (-> % second string?)
               (->> % first (get #{:id :href :stroke :fill})))
        (if-let [new-ref (->> % second (get prepared-id-map))]
          [(first %) new-ref]
          %)
        %)
     data)))

(defn make-unique-ids [data identifier]
  (let [prefix (util/sha1 identifier)
        ids (->> data
                 (tree-seq (some-fn map? vector? seq?) seq)
                 (filter #(and (vector? %)
                               (-> % first (= :id))))
                 (map second)
                 set)
        id-map (->> ids
                    (map (fn [id]
                           [id (util/id (str prefix "-unique-" id))]))
                    (into {}))]
    (-> data
        (replace-id-references id-map))))

(defn strip-style-block [data]
  (walk/postwalk (fn [value]
                   (if (and (vector? value)
                            (-> value first (= :style)))
                     nil
                     value))
                 data))

(defn strip-classes [data]
  (walk/postwalk (fn [value]
                   (if (map? value)
                     (dissoc value :class)
                     value))
                 data))
