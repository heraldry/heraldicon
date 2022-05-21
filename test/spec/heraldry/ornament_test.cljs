(ns spec.heraldry.ornament-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-ornament
  (are [form] (tu/valid? :heraldry/ornament form)
    (tu/example :heraldry/charge)

    (tu/example :heraldry/charge-group)

    (tu/example :heraldry/motto)

    (tu/example :heraldry/shield-separator)))

(deftest invalid-ornament
  (are [form] (tu/invalid? :heraldry/ornament form)
    {}

    :wrong))
