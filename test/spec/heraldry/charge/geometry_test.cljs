(ns spec.heraldry.charge.geometry-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-geometry
  (are [form] (tu/valid? :heraldry.charge/geometry form)
    {}

    {:size 5}

    {:stretch 1.2}

    {:mirrored? true}

    {:reversed? false}))

(deftest invalid-geometry
  (are [form] (tu/invalid? :heraldry.charge/geometry form)
    {:size :wrong}

    {:stretch :wrong}

    {:mirrored? :wrong}

    {:reversed? :wrong}))
