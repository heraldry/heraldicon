(ns heraldicon.localization.string
  (:require
   [clojure.string :as s]
   [heraldicon.localization.locale :as locale]))

(defn tr-raw [data language]
  (cond
    (keyword? data) (tr-raw (locale/string data) language)
    (map? data) (let [translated (get data language)]
                  (if (some-> translated count pos?)
                    translated
                    (get data :en)))
    :else (str data)))

(defn combine [separator words]
  (let [translated (->> locale/all
                        keys
                        (map (fn [language]
                               [language
                                (->> words
                                     (map (fn [s]
                                            (if (or (string? s)
                                                    (keyword? s)
                                                    (map? s))
                                              s
                                              (str s))))
                                     (map (fn [s]
                                            (tr-raw s language)))
                                     (filter #(> (count %) 0))
                                     (s/join (tr-raw separator language)))]))
                        (into {}))]
    (if (-> translated
            vals
            set
            count
            (= 1))
      (-> translated vals first)
      translated)))

(defn str-tr [& strs]
  (let [translated (->> locale/all
                        keys
                        (map (fn [language]
                               [language
                                (->> strs
                                     (map (fn [s]
                                            (tr-raw s language)))
                                     (filter #(> (count %) 0))
                                     (apply str))]))
                        (into {}))]
    (if (-> translated
            vals
            set
            count
            (= 1))
      (-> translated vals first)
      translated)))

(defn interleave-all
  "Returns a lazy seq of the first item in each coll, then the second etc.,
   unlike the clojure.core version this includes all elements."
  [c1 c2]
  (lazy-seq
   (let [s1 (seq c1) s2 (seq c2)]
     (when (or s1 s2)
       (cond->> (interleave-all (rest s1) (rest s2))
         (first s2) (cons (first s2))
         (first s1) (cons (first s1)))))))

(defn format-tr [s & args]
  (let [chunks (s/split s "%s")]
    (apply str-tr (interleave-all chunks args))))

(defn- -upper-case-first-str [s]
  (str (s/upper-case (or (first s) "")) (subs s 1)))

(defn upper-case-first [s]
  (if (map? s)
    (->> s
         (map (fn [[k v]]
                [k (-upper-case-first-str v)]))
         (into {}))
    (-upper-case-first-str s)))