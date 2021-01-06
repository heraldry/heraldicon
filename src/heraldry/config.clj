(ns heraldry.config
  (:refer-clojure :exclude [get])
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [shadow-env.core :as env]))

(defn read-env [_build-state]
  (let [aero-config {:profile (or (keyword (System/getenv "STAGE")) :local)}
        [config] (->> ["config.edn"]
                      (map #(some-> (io/resource %)
                                    (aero/read-config aero-config))))]
    {:common config
     :clj {}
     :cljs {}}))

(env/link get `read-env)

(defmacro get-static [k]
  (clojure.core/get get k))
