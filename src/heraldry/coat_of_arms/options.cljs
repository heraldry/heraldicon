(ns heraldry.coat-of-arms.options
  (:require [clojure.walk :as walk]
            [heraldry.util :as util]))

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
