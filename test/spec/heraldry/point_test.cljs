(ns spec.heraldry.point-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-point
  (are [form] (tu/valid? :heraldry/point form)
    {:x 10
     :y 5}))

(deftest invalid-point
  (are [form] (tu/invalid? :heraldry/point form)
    {}

    {:x 10}

    {:y 10}

    {:x 10
     :y :wrong}

    {:x :wrong
     :y 10}))
