(ns spec.heraldicon.entity.collection-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-collection-entity
  (are [form] (tu/valid? :heraldicon.entity/collection form)
    {:type :heraldicon.entity.type/collection
     :name "foobar"
     :data (tu/example :heraldicon.entity.collection/data)}))

(deftest invalid-collection-entity
  (are [form] (tu/invalid? :heraldicon.entity/collection form)
    {}

    {:type :heraldicon.entity.type/foo
     :name "foobar"}

    {:type :heraldicon.entity.type/collection
     :name "foobar"
     :data nil}

    {:type :heraldicon.entity.type/collection
     :name "foobar"
     :data :foobar}))
