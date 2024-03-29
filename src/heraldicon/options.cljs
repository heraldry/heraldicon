(ns heraldicon.options
  (:require
   [clojure.walk :as walk]
   [heraldicon.util.sanitize :as sanitize]))

(def ^:private option-types
  #{:option.type/range
    :option.type/choice
    :option.type/boolean
    :option.type/text})

(def manual-blazon
  {:type :option.type/text
   :default nil
   :ui/label :string.option/manual-blazon})

(def plain-outline?-option
  {:type :option.type/boolean
   :default false
   :ui/label :string.charge.tincture-modifier.special/outline})

(defn choices->map [choices]
  (into {}
        (mapcat (fn [[group-name & items]]
                  (if (and (-> items count (= 1))
                           (-> items first keyword?))
                    ;; in this case there is no group, treat the first element of "items" as key
                    ;; and "group-name" as display-name
                    [[(first items) group-name]]
                    (map (comp vec reverse) items))))
        choices))

(defn get-value [value options]
  (cond
    (and (vector? value)
         (-> value first (= :force))) (second value)
    (some? (:override options)) (:override options)
    :else (let [fallback (first (keep identity [(:inherited options)
                                                (:default options)]))
                value (first (keep identity [value
                                             fallback]))]
            (case (:type options)
              :option.type/boolean (boolean value)
              :option.type/choice (let [choices (choices->map (:choices options))]
                                    (if (contains? choices value)
                                      value
                                      (if (contains? choices fallback)
                                        fallback
                                        (-> options :choices first second))))
              :option.type/range (cond
                                   (and (nil? value)
                                        (-> options :default nil?)) nil
                                   (nil? value) (:min options)
                                   :else (-> value
                                             (max (:min options))
                                             (min (:max options))))
              :option.type/text (or value (:default options))
              nil))))

(defn- get-sanitized-value-or-nil [value options]
  (when-not (nil? value)
    (case (:type options)
      :option.type/boolean (boolean value)
      :option.type/choice (let [choices (choices->map (:choices options))]
                            (when (contains? choices value)
                              value))
      :option.type/range (when-not (nil? value)
                           (-> value
                               (max (:min options))
                               (min (:max options))))
      :option.type/text value
      nil)))

(defn sanitize [values given-options]
  (into {}
        (map (fn [[k v]]
               (cond
                 (and (map? v)
                      (not (contains?
                            option-types (:type v)))) [k (sanitize (get values k) v)]
                 :else [k (get-value (get values k) v)])))
        given-options))

(defn sanitize-value-or-data [data options]
  (when (map? options)
    ;; TODO: find better way to determine option leaf or branch
    (if (or (-> options :type not)
            (-> options :type :type))
      (sanitize data options)
      (get-value data options))))

(defn sanitize-or-nil [values given-options]
  (sanitize/remove-nil-values-and-empty-maps
   (into {}
         (map (fn [[k v]]
                (cond
                  (and (map? v)
                       (not (contains?
                             option-types (:type v)))) [k (sanitize-or-nil (get values k) v)]
                  :else [k (get-sanitized-value-or-nil (get values k) v)])))
         given-options)))

(defn pick [opts paths & values]
  (let [values (first values)
        options (loop [options {}
                       [path & rest] paths]
                  (let [next-options (assoc-in options path (get-in opts path))]
                    (if (nil? rest)
                      next-options
                      (recur next-options rest))))]
    (loop [options options
           [[key value] & rest] values]
      (let [next-options (if key
                           (assoc-in options key value)
                           options)]
        (if (nil? rest)
          next-options
          (recur next-options rest))))))

(defn override-if-exists [options path value]
  (if (get-in options path)
    (assoc-in options path value)
    options))

(defn update-if-exists [options path f]
  (if (get-in options path)
    (update-in options path f)
    options))

(defn populate-inheritance [options inherited-values]
  (into {}
        (map (fn [[k v]]
               [k (if (and (-> v :type option-types)
                           (get inherited-values k))
                    (assoc v :inherited (get inherited-values k))
                    v)]))
        options))

(defn changed? [key sanitized-data options]
  (and (get options key)
       (not= (get sanitized-data key)
             (or (-> options (get key) :inherited)
                 (-> options (get key) :default)))))

(defn map-to-interval [value from to]
  (let [value (-> value
                  (max 0)
                  (min 1))]
    (-> (- to from)
        (* value)
        (+ from))))

(def ^:private not-vector? (complement vector?))

(defn- populated-group? [v]
  (or (> (count v) 2)
      (and (vector? v)
           (-> v second vector?))))

(defn- desired-choice? [v keep-choice?]
  (let [choice (second v)]
    (and (keyword? choice)
         (keep-choice? choice))))

(defn select-choices [choices select]
  (let [keep-choice? (set select)]
    (walk/postwalk (fn [data]
                     (if (vector? data)
                       (into []
                             (keep (fn [element]
                                     (when (or (not-vector? element)
                                               (populated-group? element)
                                               (desired-choice? element keep-choice?))
                                       element)))
                             data)
                       data))
                   choices)))
