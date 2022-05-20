(ns spec.heraldry.coat-of-arms-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def ^:private example-field
  (tu/example :heraldry/field))

(deftest valid-coat-of-arms
  (are [form] (tu/valid? :heraldry/coat-of-arms form)
    {:type :heraldry/coat-of-arms
     :field example-field}

    {:type :heraldry/coat-of-arms
     :field example-field
     :manual-blazon "foobar"}))

(deftest invalid-coat-of-arms
  (are [form] (tu/invalid? :heraldry/coat-of-arms form)
    {}

    {:field example-field}

    {:type :heraldry/coat-of-arms}

    {:type :wrong
     :field example-field}

    {:type :heraldry/coat-of-arms
     :field :wrong}

    {:type :heraldry/coat-of-arms
     :field example-field
     :manual-blazon :wrong}))
