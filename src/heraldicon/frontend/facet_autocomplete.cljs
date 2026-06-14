(ns heraldicon.frontend.facet-autocomplete
  "Suggestion logic for the structured-search DSL input. Pure functions: parse
   the current token, compute matching key/value completions, and produce the
   replacement string. UI rendering lives in frontend.search-filter."
  (:require
   [clojure.string :as str]
   [heraldicon.heraldry.facets :as facets]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.heraldry.tincture :as tincture]))

(def ^:private all-tinctures
  (->> (for [[_group & entries] tincture/choices
             [_label k] entries
             :when (not= k :none)]
         (name k))
       distinct
       sort
       vec))

(def ^:private all-field-types
  (->> field.options/choices
       (mapv (fn [[_label k]] (name k)))
       sort
       vec))

(def ^:private all-ordinaries
  (->> ordinary.options/choices
       (mapv (fn [[_label k]] (name k)))
       sort
       vec))

(def ^:private all-ornaments
  ;; No central enum for these; the canonical names come from the keyword
  ;; suffixes the extractor emits (see heraldry.facets/ornament-token).
  ["compartment"
   "crown"
   "helm"
   "mantling"
   "motto"
   "slogan"
   "supporter"
   "torse"])

(def ^:private all-attitudes
  ["close"
   "combatant"
   "contourny"
   "couchant"
   "courant"
   "displayed"
   "dormant"
   "guardant"
   "hovering"
   "passant"
   "rampant"
   "regardant"
   "rising"
   "salient"
   "segreant"
   "sejant"
   "springing"
   "statant"
   "volant"])

(def ^:private values-for-key
  {"tincture" all-tinctures
   "partition" all-field-types
   "field" all-field-types
   "main-field" all-field-types
   "ordinary" all-ordinaries
   "ornament" all-ornaments
   "attitude" all-attitudes
   ;; Charge/crest values live in the database (charge_types table). No
   ;; static list — users type the slug directly.
   "charge" nil
   "crest" nil})

(def ^:private facet-keys-set
  (set facets/facet-keys))

(defn- current-token-bounds
  "Returns [start end] indices of the token between the last whitespace and
   the end of the string."
  [s]
  (let [end (count s)
        idx (str/last-index-of s " ")
        start (if idx (inc idx) 0)]
    [start end]))

(defn- current-token [s]
  (when s
    (let [[start end] (current-token-bounds s)]
      (subs s start end))))

(defn- parse-token [t]
  (let [lower (str/lower-case t)]
    (if-let [idx (str/index-of lower ":")]
      {:key (subs lower 0 idx)
       :value (subs lower (inc idx))}
      {:value lower})))

(defn suggestions
  "Returns completion strings for the user's current (cursor-at-end) token.
   Returns nil when there's nothing useful to offer."
  [input]
  (when-let [token (current-token input)]
    (when (seq token)
      (let [{:keys [key value]} (parse-token token)]
        (cond
          ;; Recognized key — offer matching values.
          (and key (facet-keys-set key))
          (when-let [vs (values-for-key key)]
            (->> vs
                 (filter #(str/starts-with? % (or value "")))
                 (mapv #(str key ":" %))))

          ;; No colon yet, or unknown key prefix — offer matching keys.
          :else
          (let [prefix (or key value "")]
            (->> facets/facet-keys
                 (filter #(str/starts-with? % prefix))
                 (mapv #(str % ":")))))))))

(defn apply-suggestion
  "Replaces the current token in `input` with `suggestion`. Returns the new
   input string."
  [input suggestion]
  (let [[start _] (current-token-bounds input)]
    (str (subs input 0 start) suggestion)))
