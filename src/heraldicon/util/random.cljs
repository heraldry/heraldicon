(ns heraldicon.util.random
  (:refer-clojure :exclude [float])
  (:require
   ["random" :as random-lib]))

(defn seed [seed]
  (.use random-lib/default seed))

(defn float []
  (.float random-lib/default))
