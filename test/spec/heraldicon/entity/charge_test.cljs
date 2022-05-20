(ns spec.heraldicon.entity.charge-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-charge-entity
  (are [form] (tu/valid? :heraldicon.entity/charge form)
    {:type :heraldicon.entity.type/charge
     :name "foobar"
     :data (tu/example :heraldicon.entity.charge/data)}))

(deftest invalid-charge-entity
  (are [form] (tu/invalid? :heraldicon.entity/charge form)
    {}

    {:type :heraldicon.entity.type/foo
     :name "foobar"}

    {:type :heraldicon.entity.type/charge
     :name "foobar"
     :data nil}

    {:type :heraldicon.entity.type/charge
     :name "foobar"
     :data :foobar}))
