(ns spec.heraldicon.entity.metadata-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-metadata
  (are [form] (tu/valid? :heraldicon.entity/metadata form)
    {}

    {"foo1" "bar1"
     "foo2" "bar2"}))

(deftest invalid-metadata
  (are [form] (tu/invalid? :heraldicon.entity/metadata form)
    {:wrong "bar"}

    {"foo" :wrong}))
