(ns heraldry.util
  (:require [clojure.pprint :refer [pprint]]))

(defn promise
  [resolver]
  (js/Promise. resolver))

(defn promise-from-callback
  [f]
  (promise (fn [resolve reject]
             (f (fn [error data]
                  (if (nil? error)
                    (resolve data)
                    (reject error)))))))

(defn spy [value msg]
  (println msg)
  (pprint value)
  value)

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
