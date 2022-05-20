(ns spec.heraldry.charge.variant-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-variant
  (are [form] (tu/valid? :heraldry.charge/variant form)
    {:id "some-id"
     :version 5}))

(deftest invalid-variant
  (are [form] (tu/invalid? :heraldry.charge/variant form)
    {}

    {:id "some-id"}

    {:version 5}

    {:id :wrong
     :version 5}

    {:id "some-id"
     :version :wrong}))
