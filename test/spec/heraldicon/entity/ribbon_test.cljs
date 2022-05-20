(ns spec.heraldicon.entity.ribbon-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-ribbon-entity
  (are [form] (tu/valid? :heraldicon.entity/ribbon form)
    {:type :heraldicon.entity.type/ribbon
     :name "foobar"
     :data (tu/example :heraldicon.entity.ribbon/data)}))

(deftest invalid-ribbon-entity
  (are [form] (tu/invalid? :heraldicon.entity/ribbon form)
    {}

    {:type :heraldicon.entity.type/foo
     :name "foobar"}

    {:type :heraldicon.entity.type/ribbon
     :name "foobar"
     :data nil}

    {:type :heraldicon.entity.type/ribbon
     :name "foobar"
     :data :foobar}))
