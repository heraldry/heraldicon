(ns heraldicon.edn
  (:require
   [cljs.reader :as reader]
   [heraldicon.math.vector :as v]))

(defn read-string [s]
  (reader/read-string {:readers {'heraldicon.math.vector.Vector v/map->Vector}} s))
