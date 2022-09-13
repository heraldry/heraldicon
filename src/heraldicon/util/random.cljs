(ns heraldicon.util.random
  (:refer-clojure :exclude [float])
  (:require
   ["random" :as random-lib]
   ["seedrandom" :as seedrandom]))

(defn seed [seed]
  (.use random-lib (seedrandom seed)))

(defn float []
  (.float random-lib))
