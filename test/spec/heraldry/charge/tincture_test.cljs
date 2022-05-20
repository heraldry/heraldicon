(ns spec.heraldry.charge.tincture-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-charge-tincture
  (are [form] (tu/valid? :heraldry.charge/tincture form)
    {:shadow 0.5
     :highlight 0.5
     :secondary :or
     :langued :gules
     :armed :vert}))

(deftest invalid-charge-tincture
  (are [form] (tu/invalid? :heraldry.charge/tincture form)
    {:shadow true}

    {:armed 14}

    {:highlight :or}))
