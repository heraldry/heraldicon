(ns heraldicon.svg.core
  (:require
   ["css" :as css]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.util.uid :as uid]
   [taoensso.timbre :as log]))

(defn- split-style-value [value]
  (into {}
        (keep (fn [chunk]
                (let [[k v] (str/split chunk #":" 2)
                      k (some-> k str/trim)
                      v (some-> v str/trim)]
                  (when-not (or (str/blank? k)
                                (str/blank? v))
                    [(keyword k) v]))))
        (str/split value #";")))

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
           :patternunits :patternUnits
           :patterntransform :patternTransform
           :gradientunits :gradientUnits
           :xlink:href :href
           :gradienttransform :gradientTransform
           :lineargradient :linearGradient
           :radialgradient :radialGradient
           :preserveaspectratio :preserveAspectRatio
           :xml:space :xmlSpace
           :calcmode :calcMode
           :repeatcount :repeatCount
           :attributename :attributeName
           :filterunits :filterUnits
           :clippathunits :clipPathUnits
           :primitiveunits :primitiveUnits
           :stddeviation :stdDeviation
           :horizOriginX :horizoriginx
           :horizOriginY :horizoriginy
           :basefrequency :baseFrequency
           :solidColor :solidcolor
           :requiredextensions :requiredExtensions} v v))
   data))

(defn- svg-namespaced-tag-or-attribute? [tag]
  (and (keyword? tag)
       (-> tag name (str/includes? ":"))))

(defn remove-namespaced-elements [data]
  (walk/postwalk
   (fn [data]
     (cond
       (vector? data) (into []
                            (remove (fn [element]
                                      (and (vector? element)
                                           (-> element first svg-namespaced-tag-or-attribute?))))
                            data)
       (map? data) (into {}
                         (remove (comp svg-namespaced-tag-or-attribute? first))
                         data)
       :else data))

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
               (-> % first #{:id :href :stroke :fill :mask :clip-path}))
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
   (-> (svgo-optimize-fn data)
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
                                        (-> % count (> 2))
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
                                             :stroke-opacity :stroke-dashoffset :stroke-linecap :stroke-linejoin
                                             :fill :fill-opacity :fill-rule :opacity :color :visibility
                                             :mask :display]
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

(defn- strip-currentcolor [data]
  (walk/postwalk (fn [value]
                   (if (map? value)
                     (into {}
                           (remove (fn [[_ v]]
                                     (and (string? v)
                                          (-> v str/lower-case (= "currentcolor")))))
                           value)
                     value))
                 data))

(defn strip-unnecessary-parts [svg-data]
  (-> svg-data
      (strip-elements #{:style :foreignObject :foreign-object :foreignobject})
      strip-switch-elements
      strip-classes-and-ids
      strip-currentcolor))

(defn add-ids [data]
  (walk/postwalk (fn [value]
                   (cond-> value
                     (and (vector? value)
                          (-> value second map?)
                          (-> value second :fill)) (assoc-in [1 :id] (uid/generate "id"))))
                 data))

(defn strip-ids [data]
  (walk/postwalk (fn [value]
                   (cond-> value
                     (and (vector? value)
                          (-> value second map?)) (update 1 dissoc :id)))
                 data))

(defn strip-clip-paths [data]
  (walk/postwalk (fn [value]
                   (if (and (vector? value)
                            (= (first value) :clip-path))
                     [:clip-path nil]
                     value))
                 data))

(defn fix-stroke-and-fill [data]
  ;; add fill and stroke at top level as default
  ;; some SVGs don't specify them for elements if
  ;; they are black, but for that to work we need
  ;; the root element to specify them
  ;; disadvantage: this colour will now always show
  ;; im the interface, even if the charge doesn't
  ;; contain and black elements, but they usually will
  ;;
  ;; the stroke-width is also set to 0, because areas
  ;; that really should not get an outline otherwise
  ;; would default to one of width 1
  (let [new-data (assoc data 1 {:fill "#000000"
                                :stroke "none"
                                :stroke-width 0})]
    (walk/postwalk
     (fn [data]
       ;; as a follow up to above comment:
       ;; if we find an element that has a stroke-width but no
       ;; stroke, then set that stroke to none, so those thick
       ;; black outlines won't appear
       ;; TODO: this might, for some SVGs, remove some outlines,
       ;; namely if the element was supposed to inherit the stroke
       ;; from a parent element
       (if (and (map? data)
                (contains? data :stroke-width)
                (not (contains? data :stroke)))
         (assoc data :stroke "none")
         data))
     new-data)))
