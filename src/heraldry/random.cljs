(ns heraldry.random
  (:require
   ["random" :as random-lib]
   ["seedrandom" :as seedrandom]))

(defn seed [seed]
  (.use random-lib (seedrandom seed)))

#_{:clj-kondo/ignore [:redefined-var]}
(defn float []
  (.float random-lib))
