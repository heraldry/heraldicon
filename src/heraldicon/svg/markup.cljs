(ns heraldicon.svg.markup
  "Writes hiccup as SVG markup without React, for exports rendered in server
  mode, where components are plain functions of their arguments. It follows
  what Reagent and React make of the hiccup, so the result renders the same;
  empty elements are written self-closing."
  (:require
   [clojure.string :as str]))

;; Reagent turns attribute names into camelCase, except data- and aria- ones,
;; and React writes these SVG properties hyphenated again; any other name stays
;; camelCase, e.g. :pattern-units is written patternUnits
(def ^:private hyphenated-attributes
  #{"accentHeight" "alignmentBaseline" "arabicForm" "baselineShift" "capHeight"
    "clipPath" "clipRule" "colorInterpolation" "colorInterpolationFilters"
    "colorProfile" "colorRendering" "dominantBaseline" "enableBackground"
    "fillOpacity" "fillRule" "floodColor" "floodOpacity" "fontFamily" "fontSize"
    "fontSizeAdjust" "fontStretch" "fontStyle" "fontVariant" "fontWeight"
    "glyphName" "glyphOrientationHorizontal" "glyphOrientationVertical"
    "horizAdvX" "horizOriginX" "imageRendering" "letterSpacing" "lightingColor"
    "markerEnd" "markerMid" "markerStart" "overlinePosition" "overlineThickness"
    "paintOrder" "pointerEvents" "renderingIntent" "shapeRendering" "stopColor"
    "stopOpacity" "strikethroughPosition" "strikethroughThickness"
    "strokeDasharray" "strokeDashoffset" "strokeLinecap" "strokeLinejoin"
    "strokeMiterlimit" "strokeOpacity" "strokeWidth" "textAnchor"
    "textDecoration" "textRendering" "transformOrigin" "underlinePosition"
    "underlineThickness" "unicodeBidi" "unicodeRange" "unitsPerEm" "vAlphabetic"
    "vHanging" "vIdeographic" "vMathematical" "vectorEffect" "vertAdvY"
    "vertOriginX" "vertOriginY" "wordSpacing" "writingMode" "xHeight"})

(def ^:private special-attributes
  {"className" "class"
   "htmlFor" "for"
   "xmlnsXlink" "xmlns:xlink"
   "xmlBase" "xml:base"
   "xmlLang" "xml:lang"
   "xmlSpace" "xml:space"
   "xlinkActuate" "xlink:actuate"
   "xlinkArcrole" "xlink:arcrole"
   "xlinkHref" "xlink:href"
   "xlinkRole" "xlink:role"
   "xlinkShow" "xlink:show"
   "xlinkTitle" "xlink:title"
   "xlinkType" "xlink:type"})

;; CSS properties React leaves without px when the value is a number
(def ^:private unitless-styles
  #{"animationIterationCount" "aspectRatio" "borderImageOutset"
    "borderImageSlice" "borderImageWidth" "boxFlex" "boxFlexGroup"
    "boxOrdinalGroup" "columnCount" "columns" "flex" "flexGrow" "flexPositive"
    "flexShrink" "flexNegative" "flexOrder" "gridArea" "gridRow" "gridRowEnd"
    "gridRowSpan" "gridRowStart" "gridColumn" "gridColumnEnd" "gridColumnSpan"
    "gridColumnStart" "fontWeight" "lineClamp" "lineHeight" "opacity" "order"
    "orphans" "scale" "tabSize" "widows" "zIndex" "zoom" "fillOpacity"
    "floodOpacity" "stopOpacity" "strokeDasharray" "strokeDashoffset"
    "strokeMiterlimit" "strokeOpacity" "strokeWidth"})

(defn- escape [s]
  ;; path data is most of the text and has nothing to escape
  (if (re-find #"[&<>\"']" s)
    (-> s
        (str/replace "&" "&amp;")
        (str/replace "<" "&lt;")
        (str/replace ">" "&gt;")
        (str/replace "\"" "&quot;")
        (str/replace "'" "&#x27;"))
    s))

(defn- hyphenate [s]
  (str/replace s #"[A-Z]" #(str "-" (str/lower-case %))))

(defn- camelize [s]
  (str/replace s #"-([a-z])" #(str/upper-case (second %))))

(defn- cached
  "f memoized on a string, for the few names repeated across a whole export."
  [f]
  (let [cache (js/Map.)]
    (fn [s]
      (or (.get cache s)
          (let [result (f s)]
            (.set cache s result)
            result)))))

(def ^:private attribute-name*
  (cached (fn [n]
            (let [camel (if (or (str/starts-with? n "data-")
                                (str/starts-with? n "aria-"))
                          n
                          (camelize n))]
              (or (special-attributes camel)
                  (if (hyphenated-attributes camel)
                    (hyphenate camel)
                    camel))))))

(defn- attribute-name [k]
  (attribute-name* (name k)))

(def ^:private style-name
  (cached (fn [n]
            (let [camel (camelize n)]
              [(hyphenate camel) (contains? unitless-styles camel)]))))

;; Reagent's tag shorthand: :tag#id.class1.class2
(def ^:private parse-tag
  (cached (fn [n]
            (let [[_ tag-name id classes] (re-matches #"([^.#]+)(?:#([^.]+))?(?:\.(.+))?" n)]
              [tag-name id (some-> classes (str/split #"\."))]))))

(defn- value-string [v]
  (if (keyword? v)
    (name v)
    (str v)))

(defn- style-string [style]
  (->> style
       (keep (fn [[k v]]
               (when (some? v)
                 (let [[css-name unitless?] (style-name (name k))]
                   (str css-name
                        ":"
                        (if (and (number? v)
                                 (not (zero? v))
                                 (not unitless?))
                          (str v "px")
                          (value-string v)))))))
       (str/join ";")))

(defn- attribute [[k v]]
  (when-not (or (nil? v)
                (boolean? v)
                (fn? v)
                (#{:key :ref :dangerouslySetInnerHTML} k))
    (let [n (attribute-name k)
          value (cond
                  (= n "style") (if (map? v) (style-string v) (str v))
                  (and (= n "class")
                       (sequential? v)) (str/join " " (remove nil? v))
                  :else (value-string v))]
      (str " " n "=\"" (escape value) "\""))))

(defn- component? [x]
  (or (fn? x)
      (instance? MultiFn x)))

(declare emit!)

(defn- emit-element! [^js out [tag & more]]
  (let [[tag-name id classes] (parse-tag (name tag))
        attributes (when (map? (first more))
                     (first more))
        children (if (or attributes
                         (nil? (first more)))
                   (rest more)
                   more)
        attributes (cond-> attributes
                     (and id (not (:id attributes))) (assoc :id id)
                     classes (update :class #(str/join " " (remove nil? (cons % classes)))))
        inner-html (some-> attributes :dangerouslySetInnerHTML :__html)]
    (.push out (str "<" tag-name (apply str (keep attribute attributes))))
    (cond
      inner-html (.push out (str ">" inner-html "</" tag-name ">"))
      (some some? children) (do
                              (.push out ">")
                              (doseq [child children]
                                (emit! out child))
                              (.push out (str "</" tag-name ">")))
      :else (.push out "/>"))))

(defn- emit! [^js out form]
  (cond
    (or (nil? form)
        (boolean? form)) nil
    (string? form) (.push out (escape form))
    (number? form) (.push out (str form))
    (vector? form) (let [tag (first form)]
                     (cond
                       (= tag :<>) (doseq [child (rest form)]
                                     (emit! out child))
                       (keyword? tag) (emit-element! out form)
                       ;; a component is called with its arguments; a form-2
                       ;; component returns the function that renders
                       (component? tag) (let [args (rest form)
                                              result (apply tag args)]
                                          (emit! out (if (fn? result)
                                                       (apply result args)
                                                       result)))
                       :else (throw (ex-info "Unsupported hiccup" {:form form}))))
    (sequential? form) (doseq [child form]
                         (emit! out child))
    :else (.push out (escape (str form)))))

(defn render
  "The markup for hiccup, with its components expanded."
  [hiccup]
  (let [out #js []]
    (emit! out hiccup)
    (.join out "")))
