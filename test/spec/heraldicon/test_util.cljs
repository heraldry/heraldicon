(ns spec.heraldicon.test-util
  (:require
   [clansi.core :as cla]
   [cljs.spec.alpha :as s]
   [clojure.test.check.generators :as gen]))

(defn valid? [spec form]
  (let [valid? (s/valid? spec form)]
    (when-not valid?
      (println (cla/style (with-out-str (s/explain spec form)) :magenta)))
    valid?))

(defn invalid? [spec form]
  (not (s/valid? spec form)))

(defn example [spec]
  (first (gen/sample (s/gen spec) 1)))
