(ns spec.heraldicon.entity.arms-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-arms-entity
  (are [form] (tu/valid? :heraldicon.entity/arms form)
    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :data (tu/example :heraldicon.entity.arms/data)}))

(deftest invalid-arms-entity
  (are [form] (tu/invalid? :heraldicon.entity/arms form)
    {}

    {:type :heraldicon.entity.type/foo
     :name "foobar"}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :data nil}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :data :foobar}))
