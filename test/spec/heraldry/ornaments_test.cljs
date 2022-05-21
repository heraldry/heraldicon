(ns spec.heraldry.ornaments-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-ornaments
  (are [form] (tu/valid? :heraldry/ornaments form)
    {:type :heraldry/ornaments}

    {:type :heraldry/ornaments
     :elements []}

    {:type :heraldry/ornaments
     :elements [(tu/example :heraldry/ornament)]}))

(deftest invalid-ornaments
  (are [form] (tu/invalid? :heraldry/ornaments form)
    {}

    {:type :wrong}

    {:type :heraldry/ornaments
     :elements :wrong}

    {:type :heraldry/ornaments
     :elements [:wrong]}))
