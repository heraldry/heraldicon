(ns spec.heraldry.motto.ribbon-variant-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-ribbon-variant
  (are [form] (tu/valid? :heraldry.motto/ribbon-variant form)
    {:id "some-id"
     :version 5}))

(deftest invalid-ribbon-variant
  (are [form] (tu/invalid? :heraldry.motto/ribbon-variant form)
    {}

    {:id "some-id"}

    {:version 5}

    {:id :wrong
     :version 5}

    {:id "some-id"
     :version :wrong}))
