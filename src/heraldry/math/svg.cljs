(ns heraldry.math.svg
  (:require ["svg-path-parse" :as svg-path-parse]
            ["svg-path-properties" :as svg-path-properties]
            ["svg-path-reverse" :as svg-path-reverse]
            ["svgpath" :as svgpath]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.math.catmullrom :as catmullrom]
            [heraldry.random :as random]
            [heraldry.math.vector :as v]
            [heraldry.util :as util]))

(defn clean-path [d]
  (s/replace d #"l *0 *[, ] *0" ""))

(defn new-path [d]
  (->> d
       clean-path
       (new svg-path-properties/svgPathProperties)))

(defn points [^js/Object path n]
  (let [length (.getTotalLength path)
        n (if (= n :length)
            (-> length
                Math/floor
                inc)
            n)]
    (mapv (fn [i]
            (let [x (-> length (* i) (/ (dec n)))
                  p (.getPointAtLength path x)]
              (v/v (.-x p) (.-y p)))) (range n))))

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
  (let [path (new-path d)
        points (points path 50)
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

(defn center [d]
  (let [path (new-path d)
        points (points path 50)
        center (avg-x-y points)]
    center))

(defn make-path [v]
  (cond
    (string? v) v
    (and (map? v)
         (:x v)
         (:y v)) (str (:x v) "," (:y v))
    (sequential? v) (s/join " " (map make-path v))
    :else (str v)))

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

(defn jiggle [[previous
               {:keys [x y] :as current}
               _]]
  (let [dist (-> current
                 (v/sub previous)
                 (v/abs))
        jiggle-radius (/ dist 4)
        dx (- (* (random/float) jiggle-radius)
              jiggle-radius)
        dy (- (* (random/float) jiggle-radius)
              jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn -squiggly-path [path & {:keys [seed]}]
  (random/seed (if seed
                 [seed path]
                 path))
  (let [points (-> path
                   new-path
                   (points :length))
        points (vec (concat [(first points)]
                            (map jiggle (partition 3 1 points))
                            [(last points)]))
        curve (catmullrom/catmullrom points)
        new-path (catmullrom/curve->svg-path-relative curve)]
    new-path))

(def squiggly-path
  (memoize -squiggly-path))

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (squiggly-path (second v))]
                                     v))))
                 data))

(defn translate [path dx dy]
  (-> path
      svgpath
      (.translate dx dy)
      .toString))

(defn stitch [path]
  ;; TODO: this can be improved, it already broke some things and caused unexpected behaviour,
  ;; because the 'e' was not part of the pattern
  (s/replace path #"^M[ ]*[0-9.e-]+[, -] *[0-9.e-]+" ""))

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

(defn reverse-path [path]
  (-> path
      svg-path-reverse/reverse
      svg-path-parse/pathParse
      .relNormalize
      (js->clj :keywordize-keys true)
      (as-> path
            (let [[move & rest] (:segments path)
                  [x y] (:args move)
                  adjusted-path (assoc path :segments (into [{:type "M" :args [0 0]}] rest))]
              {:start (v/v x y)
               :path (-> adjusted-path
                         clj->js
                         svg-path-parse/serializePath)}))))

(defn normalize-path-relative [path]
  (-> path
      svg-path-reverse/reverse
      svg-path-reverse/reverse
      svg-path-parse/pathParse
      .relNormalize
      svg-path-parse/serializePath))

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
