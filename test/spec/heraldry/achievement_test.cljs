(ns spec.heraldry.achievement-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def example-coat-of-arms
  (tu/example :heraldry/coat-of-arms))

(deftest valid-coat-of-arms
  (are [form] (tu/valid? :heraldry/achievement form)
    {:type :heraldry/achievement
     :coat-of-arms example-coat-of-arms
     :render-options (tu/example :heraldry/render-options)
     :helms (tu/example :heraldry/helms)
     :ornaments (tu/example :heraldry/ornaments)}))

(deftest invalid-coat-of-arms
  (are [form] (tu/invalid? :heraldry/achievement form)
    {}))
