(ns heraldry.options
  (:require [clojure.walk :as walk]
            [heraldry.util :as util]))

(def option-types #{:range :choice :boolean :text})

(defn get-value [value options]
  (if (and (vector? value)
           (-> value first (= :force)))
    (second value)
    (let [value (first (keep identity [value
                                       (:inherited options)
                                       (:default options)]))]
      (case (:type options)
        :boolean (boolean value)
        :choice (let [choices (util/choices->map (:choices options))]
                  (if (contains? choices value)
                    value
                    (-> options :choices first second)))
        :range (cond
                 (and (nil? value)
                      (-> options :default nil?)) nil
                 (nil? value) (:min options)
                 :else (-> value
                           (max (:min options))
                           (min (:max options))))
        :text (or value (:default options))
        nil))))

(defn get-sanitized-value-or-nil [value options]
  (if (nil? value)
    nil
    (case (:type options)
      :boolean (boolean value)
      :choice (let [choices (util/choices->map (:choices options))]
                (if (contains? choices value)
                  value
                  nil))
      :range (if (nil? value)
               nil
               (-> value
                   (max (:min options))
                   (min (:max options))))
      :text value
      nil)))

(defn sanitize [values given-options]
  (into {}
        (for [[k v] given-options]
          (cond
            (and (map? v)
                 (not (contains?
                       option-types (:type v)))) [k (sanitize (get values k) v)]
            :else [k (get-value (get values k) v)]))))

(defn sanitize-value-or-data [data options]
  (when (map? options)
    ;; TODO: find better way to determine option leaf or branch
    (if (or (-> options :type not)
            (-> options :type :type))
      (sanitize data options)
      (get-value data options))))

(defn remove-nil-values-and-empty-maps [m]
  (walk/postwalk #(if (map? %)
                    (into {} (filter (fn [[_ v]] (not (or (nil? v)
                                                          (and (map? v)
                                                               (empty? v))))) %))
                    %)
                 m))

(defn sanitize-or-nil [values given-options]
  (-> {}
      (into (for [[k v] given-options]
              (cond
                (and (map? v)
                     (not (contains?
                           option-types (:type v)))) [k (sanitize-or-nil (get values k) v)]
                :else [k (get-sanitized-value-or-nil (get values k) v)])))
      remove-nil-values-and-empty-maps))

(defn pick [opts paths & values]
  (let [values (first values)
        options (loop [options {}
                       [path & rest] paths]
                  (let [next-options (-> options
                                         (assoc-in path (get-in opts path)))]
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

(defn populate-inheritance [options inherited-values]
  (->> options
       (map (fn [[k v]]
              [k
               (if (and (-> v :type #{:choice :range :boolean :text})
                        (get inherited-values k))
                 (assoc v :inherited (get inherited-values k))
                 v)]))
       (into {})))

(defn changed? [key sanitized-data options]
  (and (get options key)
       (not= (get sanitized-data key)
             (or (-> options (get key) :inherited)
                 (-> options (get key) :default)))))

