(ns spec.heraldry.motto.geometry-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-geometry
  (are [form] (tu/valid? :heraldry.motto/geometry form)
    {}

    {:size 5}))

(deftest invalid-geometry
  (are [form] (tu/invalid? :heraldry.motto/geometry form)
    {:size :wrong}))
