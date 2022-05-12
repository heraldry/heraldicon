(ns heraldicon.util.core
  (:require
   [cljs-time.core :as time]
   [cljs-time.format :as format]))

(defn deep-merge-with [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn xor [a b]
  (or (and a (not b))
      (and (not a) b)))

(defn keyword->str [k]
  (-> k
      str
      (subs 1)))

(defn index-of [item coll]
  (count (take-while (partial not= item) coll)))

(defn integer-string? [s]
  (re-matches #"^[0-9]+$" s))

(defn map-keys
  "Applies f to each key of m. Also to keys of m's vals and so on."
  [f m]
  (zipmap
   (map (fn [k]
          (f k))
        (keys m))
   (map (fn [v]
          (if (map? v)
            (map-keys f v)
            v))
        (vals m))))

(defn iso-now []
  (->> (time/time-now)
       (format/unparse (:date-time format/formatters))))
