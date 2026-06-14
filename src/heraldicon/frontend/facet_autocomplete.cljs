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

(def ^:private all-field-and-tincture
  ;; field: and main-field: tokens cover both the field's *type* (per-pale,
  ;; chequy, plain, …) and its *tincture* (or, azure, …). The extractor
  ;; emits both, so the suggestion list does too.
  (->> (concat all-field-types all-tinctures)
       distinct
       sort
       vec))

(def ^:private values-for-key
  {"tincture" all-tinctures
   "partition" all-field-types
   "field" all-field-and-tincture
   "main-field" all-field-and-tincture
   "ordinary" all-ordinaries
   "ornament" all-ornaments
   "attitude" all-attitudes
   ;; Charge/crest values live in the database (charge_types table). No
   ;; static list — users type the slug directly.
   "charge" nil
   "crest" nil})

(def ^:private facet-keys-set
  (set facets/facet-keys))

(def ^:private tree-picker-keys
  "Keys whose values come from the charge_types tree, not a static list."
  #{"charge" "crest"})

(defn- current-token-bounds
  "Returns [start end] indices of the whitespace-delimited token under the
   cursor. The cursor defaults to end-of-string when not provided."
  ([s] (current-token-bounds s (count (or s ""))))
  ([s cursor]
   (let [s (or s "")
         len (count s)
         cursor (max 0 (min len cursor))
         left-idx (str/last-index-of (subs s 0 cursor) " ")
         start (if left-idx (inc left-idx) 0)
         right-idx (str/index-of (subs s cursor) " ")
         end (if right-idx (+ cursor right-idx) len)]
     [start end])))

(defn- current-token
  ([s] (current-token s (count (or s ""))))
  ([s cursor]
   (when s
     (let [[start end] (current-token-bounds s cursor)]
       (subs s start end)))))

(defn- parse-token [t]
  (let [lower (str/lower-case t)]
    (if-let [idx (str/index-of lower ":")]
      {:key (subs lower 0 idx)
       :value (subs lower (inc idx))}
      {:value lower})))

(defn suggestions
  "Returns completion strings for the token under the cursor. Returns nil
   when there's nothing useful to offer."
  ([input] (suggestions input (count (or input ""))))
  ([input cursor]
   (when-let [token (current-token input cursor)]
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
                  (mapv #(str % ":"))))))))))

(defn apply-suggestion
  "Replaces the token under the cursor in `input` with `suggestion`, leaving
   any text after the token untouched. Returns [new-input new-cursor], with
   the cursor parked at the end of the inserted text."
  ([input suggestion] (apply-suggestion input (count (or input "")) suggestion))
  ([input cursor suggestion]
   (let [[start end] (current-token-bounds input cursor)
         new-input (str (subs input 0 start) suggestion (subs input end))
         new-cursor (+ start (count suggestion))]
     [new-input new-cursor])))

(defn tree-key
  "If the token under the cursor uses a key whose values live in the
   charge-type tree (charge, crest), returns that key string. Otherwise nil
   — the caller should fall back to the static suggestion list."
  ([input] (tree-key input (count (or input ""))))
  ([input cursor]
   (when-let [token (current-token input cursor)]
     (let [{:keys [key]} (parse-token token)]
       (tree-picker-keys key)))))

(defn current-value
  "Returns the value portion of the token under the cursor, or nil if the
   token has no colon yet. Used to drive tree pre-selection for charge: /
   crest: tokens."
  ([input] (current-value input (count (or input ""))))
  ([input cursor]
   (when-let [token (current-token input cursor)]
     (:value (parse-token token)))))
