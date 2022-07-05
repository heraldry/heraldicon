(ns heraldicon.svg.core
  (:require
   ["css" :as css]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.util.uid :as uid]
   [taoensso.timbre :as log]))

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
     (get {:pattern-units :patternUnits
           :gradientunits :gradientUnits
           :xlink:href :href
           :gradienttransform :gradientTransform
           :lineargradient :linearGradient
           :radialgradient :radialGradient
           :preserveaspectratio :preserveAspectRatio
           :xml:space :xmlSpace} v v))
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

(defn optimize [data svgo-optimize-fn]
  (go-catch
   (-> {:removeUnknownsAndDefaults false
        :convertStyleToAttrs false}
       clj->js
       (svgo-optimize-fn data)
       <?
       (js->clj :keywordize-keys true)
       :data)))

(defn- parse-css-rules [css]
  (try
    (let [parsed (-> (css/parse css)
                     (js->clj :keywordize-keys true))]
      (mapv (fn [{:keys [selectors declarations]}]
              [selectors
               (into {}
                     (map (fn [{:keys [property value]}]
                            [(keyword property) value]))
                     declarations)])
            (-> parsed :stylesheet :rules)))
    (catch :default e
      (log/error e))))

(defn- read-css-rules [svg-data]
  (let [style-blocks (->> svg-data
                          (tree-seq (some-fn map? vector? seq?) seq)
                          (filter #(and (vector? %)
                                        (-> % first (= :style))
                                        (-> % second map?)
                                        (-> % second :type (= "text/css")))))
        css-data (->> style-blocks
                      (map (fn [style]
                             (nth style 2)))
                      (apply str))]
    (parse-css-rules css-data)))

(defn- css-rule-applicable? [selectors tag css-class css-id]
  (some (fn [selector]
          ;; limit to .class, #id, tag; more complex selectors are
          ;; difficult and likely not needed
          (or (= selector (str "." css-class))
              (= selector (str "#" css-id))
              (= selector tag)))
        selectors))

(defn- apply-css-rule [data [selectors declarations]]
  (let [tag (-> data first name)
        css-class (-> data second :class)
        css-id (-> data second :id)]
    (cond-> data
      (css-rule-applicable?
       selectors tag css-class css-id) (update 1 (fn [attributes]
                                                   (merge declarations attributes))))))

(defn- apply-style-attributes [data]
  (let [style (-> data second :style)]
    (if style
      (loop [[style-attribute & attributes] [:stroke :stroke-width :stroke-dasharray :stroke-miterlimit
                                             :stroke-opacity :stroke-dashoffset
                                             :fill :fill-opacity]
             element data]
        (if style-attribute
          (recur attributes
                 (cond-> element
                   (get style style-attribute) (assoc-in [1 style-attribute] (get style style-attribute))))
          element))
      data)))

(defn- apply-css-rules [data css-rules]
  (if (and (vector? data)
           (-> data second map?))
    (apply-style-attributes
     (loop [result data
            [rule & remaining-rules] css-rules]
       (let [new-result (apply-css-rule result rule)]
         (if (seq remaining-rules)
           (recur new-result remaining-rules)
           new-result))))
    data))

(defn process-style-blocks [svg-data]
  (let [css-rules (read-css-rules svg-data)
        ordered-css-rules (reverse css-rules)]
    (walk/postwalk #(apply-css-rules % ordered-css-rules) svg-data)))

(defn- strip-elements [data tags]
  (walk/postwalk (fn [value]
                   (when-not (and (vector? value)
                                  (contains? tags (first value)))
                     value))
                 data))

(defn- strip-classes-and-ids [data]
  (walk/postwalk (fn [value]
                   (if (map? value)
                     (dissoc value :class :id)
                     value))
                 data))

(defn- strip-switch-elements [data]
  (walk/postwalk (fn [value]
                   (cond-> value
                     (and (vector? value)
                          (= (first value) :switch)) last))
                 data))

(defn strip-unnecessary-parts [svg-data]
  (-> svg-data
      (strip-elements #{:style :foreignObject :foreign-object :foreignobject})
      strip-switch-elements
      strip-classes-and-ids))

(defn add-ids [data]
  (walk/postwalk (fn [value]
                   (cond-> value
                     (and (vector? value)
                          (-> value second map?)
                          (-> value second :fill)) (assoc-in [1 :id] (uid/generate "id"))))
                 data))
