(ns heraldry.math.svg.core
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.math.svg.path :as path]
            [heraldry.math.vector :as v]
            [heraldry.util :as util]))

(defn min-max-x-y [[{x :x y :y} & rest]]
  (reduce (fn [[min-x max-x min-y max-y] {x :x y :y}]
            [(min min-x x)
             (max max-x x)
             (min min-y y)
             (max max-y y)])
          [x x y y]
          rest))

(defn avg-x-y [[p & rest]]
  (let [[s n] (reduce (fn [[s n] p]
                        [(v/add s p)
                         (inc n)])
                      [p 1]
                      rest)]
    (v/div s n)))

(defn bounding-box-from-path [d]
  (let [path (path/new-path d)
        points (path/points path 50)
        box (min-max-x-y points)]
    box))

(defn bounding-box [points]
  (min-max-x-y points))

(defn combine-bounding-boxes [[[first-min-x first-max-x
                                first-min-y first-max-y] & rest]]
  (reduce (fn [[min-x max-x min-y max-y]
               [next-min-x next-max-x next-min-y next-max-y]]
            [(min min-x next-min-x)
             (max max-x next-max-x)
             (min min-y next-min-y)
             (max max-y next-max-y)])
          [first-min-x first-max-x
           first-min-y first-max-y]
          rest))

(defn rotated-bounding-box [{x1 :x y1 :y :as p1} {x2 :x y2 :y :as p2} rotation & {:keys [middle scale]}]
  (let [middle (or middle
                   (v/avg p1 p2))
        scale (or scale
                  (v/v 1 1))
        points [(v/add middle
                       (v/rotate (v/dot (v/sub (v/v x1 y1)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/v x2 y1)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/v x1 y2)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/v x2 y2)
                                               middle)
                                        scale) rotation))]]
    (bounding-box points)))

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
                          :radialgradient :radialGradient} v v)
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

(defn make-unique-ids [data]
  (let [ids (->> data
                 (tree-seq (some-fn map? vector? seq?) seq)
                 (filter #(and (vector? %)
                               (-> % first (= :id))))
                 (map second)
                 set)
        id-map (->> ids
                    (map (fn [id]
                           [id (util/id (str "unique-" id))]))
                    (into {}))]
    (-> data
        (replace-id-references id-map))))
