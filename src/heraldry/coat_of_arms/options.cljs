(ns heraldry.coat-of-arms.options
  (:require [clojure.walk :as walk]
            [heraldry.util :as util]
            [taoensso.timbre :as log]))

(def types #{:range :choice :boolean})

(defn get-value [value options]
  (let [value (or value (:default options))]
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
      nil)))

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
      nil)))

(defn sanitize [values given-options]
  (into {}
        (for [[k v] given-options]
          (cond
            (and (map? v)
                 (not (contains?
                       types (:type v)))) [k (sanitize (get values k) v)]
            :else [k (get-value (get values k) v)]))))

(defn remove-nil-values [m]
  (walk/postwalk #(if (map? %)
                    (into {} (filter (fn [[_ v]] (some? v)) %))
                    %)
                 m))

(defn sanitize-or-nil [values given-options]
  (-> {}
      (into (for [[k v] given-options]
              (cond
                (and (map? v)
                     (not (contains?
                           types (:type v)))) [k (sanitize-or-nil (get values k) v)]
                :else [k (get-sanitized-value-or-nil (get values k) v)])))
      remove-nil-values))

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

(defn leaf-option-paths [m]
  (if (or (not (map? m))
          ;; TODO: maybe use a namespace here as well
          (-> m :type #{:choice :range :boolean :text}))
    {[] m}
    (into {}
          (for [[k v] m
                [ks v'] (leaf-option-paths v)]
            (when (and (:ui v')
                       (:type v'))
              [(cons k ks) v'])))))

(defn starts-with [prefix-path path]
  (= (take (count prefix-path) path)
     prefix-path))

(defn populate-inheritance [options target-path source-path]
  (let [all-options (leaf-option-paths options)
        all-options (keys all-options)
        source-options (filter (partial starts-with source-path) all-options)
        target-options (set (filter (partial starts-with target-path) all-options))
        inheritable-options (keep (fn [path]
                                    (let [relative-path (drop (count source-path) path)]
                                      (when (->> relative-path
                                                 (concat target-path)
                                                 (get target-options))
                                        relative-path))) source-options)
        offset (- (count target-path))]
    (loop [options options
           [relative-path & rest] inheritable-options]
      (if (not relative-path)
        options
        (recur
         (assoc-in options (concat target-path relative-path [:ui :inherit-from]) [offset relative-path])
         rest)))))
