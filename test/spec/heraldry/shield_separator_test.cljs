(ns spec.heraldry.shield-separator-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-shield-separator
  (are [form] (tu/valid? :heraldry/shield-separator form)
    {:type :heraldry/shield-separator}))

(deftest invalid-shield-separator
  (are [form] (tu/invalid? :heraldry/shield-separator form)
    {}

    {:type :wrong}))
