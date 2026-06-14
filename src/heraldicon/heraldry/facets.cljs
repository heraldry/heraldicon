(ns heraldicon.heraldry.facets
  "Flat 'key:value' tokens describing the contents of an arms achievement.
   Used by structured search (DSL like 'tincture:or charge:lion') and stored
   denormalized in the database with a GIN index.

   The tokens emitted here are everything derivable from the arms data
   itself. Charge-type-tree ancestors (charge:mammal, charge:animal when a
   :heraldry.charge.type/lion variant is referenced) live in the database and
   are appended by the backend save path; see backend.api.entity."
  (:require
   [clojure.set :as set]
   [clojure.string :as str]))

(defn slugify-name
  "Canonicalizes a free-form name (e.g. a charge type name from the
   charge_types table) into a facet-token-safe slug: lowercase, whitespace
   collapsed to single dashes, no leading/trailing dashes."
  [s]
  (when s
    (-> s
        str/lower-case
        (str/replace #"\s+" "-")
        (str/replace #"^-+|-+$" ""))))

(defn charge-token
  "Returns 'charge:<slug>' for a charge type name."
  [charge-type-name]
  (str "charge:" (slugify-name charge-type-name)))

(def facet-keys
  "Vocabulary of supported DSL keys. The order is the display order in
   help/autocomplete."
  ["tincture"
   "partition"
   "field"
   "main-field"
   "charge"
   "crest"
   "ordinary"
   "ornament"
   "attitude"])

(def ^:private facet-keys-set
  (set facet-keys))

(defn- facet-token? [s]
  (when-let [idx (str/index-of s ":")]
    (and (pos? idx)
         (< idx (dec (count s)))
         (facet-keys-set (subs s 0 idx)))))

(defn split-input
  "Pulls 'key:value' facet tokens out of a raw search string.
   Returns {:facets [lower-cased tokens] :phrase-text \"<remainder>\"}.
   The caller is expected to feed :phrase-text into its own phrase tokenizer
   for further normalization (case folding, accent stripping, quote handling)."
  [s]
  (let [tokens (or (some->> s (re-seq #"\"[^\"]+\"|\S+")) [])
        {facets true phrases false}
        (group-by (fn [t]
                    (boolean (facet-token? (str/lower-case t))))
                  tokens)]
    {:facets (mapv str/lower-case (or facets []))
     :phrase-text (str/join " " (or phrases []))}))

(def version
  "Bump when the extraction logic changes — rows with a lower
   :facets_version become eligible for re-extraction by the backfill."
  2)

(def ^:private crest-function
  :heraldry.charge.function/crest-charge)

(def ^:private uninteresting-tinctures
  #{:none :mixed})

(defn- type-suffix [kw]
  (some-> kw name))

(defn- type-ns [kw]
  (some-> kw namespace))

(defn- field-type-suffix
  "Returns 'per-pale' for :heraldry.field.type/per-pale, otherwise nil."
  [t]
  (when (and (keyword? t)
             (= (type-ns t) "heraldry.field.type"))
    (type-suffix t)))

(defn- map-nodes
  "Sequence of every map encountered while walking the EDN tree."
  [data]
  (filter map?
          (tree-seq #(or (map? %) (vector? %) (seq? %)) seq data)))

(defn- emit-from-node [tokens node]
  (let [t (:type node)
        tnc (:tincture node)
        att (:attitude node)
        ns (type-ns t)
        crest? (= (:function node) crest-function)
        tokens (cond-> tokens
                 (and (keyword? tnc)
                      (not (uninteresting-tinctures tnc)))
                 (conj (str "tincture:" (name tnc)))

                 (= ns "heraldry.charge.type")
                 (conj (str "charge:" (type-suffix t)))

                 (and crest?
                      (= ns "heraldry.charge.type"))
                 (conj (str "crest:" (type-suffix t)))

                 (= ns "heraldry.ordinary.type")
                 (conj (str "ordinary:" (type-suffix t)))

                 (keyword? att)
                 (conj (str "attitude:" (name att))))]
    (if-let [field-type (field-type-suffix t)]
      (cond-> (-> tokens
                  (conj (str "partition:" field-type))
                  (conj (str "field:" field-type)))
        (and (keyword? tnc)
             (not (uninteresting-tinctures tnc)))
        (conj (str "field:" (name tnc))))
      tokens)))

(defn- main-field-tokens [main-field]
  (let [field-type (field-type-suffix (:type main-field))
        tnc (:tincture main-field)]
    (cond-> #{}
      field-type (conj (str "main-field:" field-type))

      (and (keyword? tnc)
           (not (uninteresting-tinctures tnc)))
      (conj (str "main-field:" (name tnc))))))

(defn- ornament-token
  "ornament:<suffix> using the type-keyword's name (e.g. :heraldry.motto.type/motto
   → 'ornament:motto', :heraldry.charge.type/crown → 'ornament:crown',
   :heraldry/helm → 'ornament:helm')."
  [element]
  (when-let [t (:type element)]
    (when (keyword? t)
      (str "ornament:" (name t)))))

(defn- ornament-tokens
  "Both :ornaments and :helms are containers shaped
   {:type :heraldry/(ornaments|helms) :elements [...]} — the actual entries
   live under :elements. Empty elements means no ornament/helm tokens."
  [achievement]
  (into #{}
        (keep ornament-token)
        (concat (-> achievement :ornaments :elements)
                (-> achievement :helms :elements))))

(defn facets-of
  "Returns a sorted vector of facet tokens extracted from an arms entity's
   :data map. Tokens are flat 'key:value' strings.

   Does NOT include charge-type-tree ancestors (charge:mammal for a lion,
   crest:mammal for a crest-charge lion); the backend resolves those from
   arms_charges + charge_types (see backend.api.entity)."
  [arms-data]
  (let [achievement (:achievement arms-data)
        main-field (-> achievement :coat-of-arms :field)
        walk-tokens (reduce emit-from-node #{} (map-nodes achievement))]
    (-> walk-tokens
        (set/union (main-field-tokens main-field))
        (set/union (ornament-tokens achievement))
        sort
        vec)))

(defn crest-variants
  "Returns the set of charge variants ({:id <prefixed-id> :version N}) in the
   achievement that are tagged as crest charges. Used by the backend to expand
   crest tokens up the charge-type tree."
  [arms-data]
  (into #{}
        (keep (fn [node]
                (when (and (= (:function node) crest-function)
                           (= (type-ns (:type node)) "heraldry.charge.type")
                           (-> node :variant :id))
                  (:variant node))))
        (map-nodes (:achievement arms-data))))
