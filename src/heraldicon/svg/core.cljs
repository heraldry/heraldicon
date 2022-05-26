(ns heraldicon.svg.core
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.util.uid :as uid]))

(defn- split-style-value [value]
  (into {}
        (keep (fn [chunk]
                (let [[k v] (s/split chunk #":" 2)
                      k (some-> k s/trim)
                      v (some-> v s/trim)]
                  (when (and (some-> k count pos?)
                             (some-> v count pos?))
                    [(keyword k) v]))))
        (s/split value #";")))

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
                          :preserveaspectratio :preserveAspectRatio
                          :xml:space :xmlSpace} v v)
       :else v))
   data))

(defn- replace-id-references [data id-map]
  (let [prepared-id-map (into {}
                              (mapcat (fn [[k v]]
                                        [[k v]
                                         [(str "#" k) (str "#" v)]
                                         [(str "url(#" k ")") (str "url(#" v ")")]]))
                              id-map)]
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
        id-map (into {}
                     (map (fn [id]
                            [id (uid/generate (str "unique-" id))]))
                     ids)]
    (replace-id-references data id-map)))

(defn strip-style-block [data]
  (walk/postwalk (fn [value]
                   (when-not (and (vector? value)
                                  (-> value first (= :style)))
                     value))
                 data))

(defn strip-classes [data]
  (walk/postwalk (fn [value]
                   (if (map? value)
                     (dissoc value :class)
                     value))
                 data))

(defn optimize [data svgo-optimize-fn]
  (go-catch
   (-> {:removeUnknownsAndDefaults false}
       clj->js
       (svgo-optimize-fn data)
       <?
       (js->clj :keywordize-keys true)
       :data)))
