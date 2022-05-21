(ns spec.heraldicon.entity.arms.data-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-arms-data
  (are [form] (tu/valid? :heraldicon.entity.arms/data form)
    {:type :heraldicon.entity.arms/data
     :achievement (tu/example :heraldry/achievement)}))

(deftest invalid-arms-data
  (are [form] (tu/invalid? :heraldicon.entity.arms/data form)
    {}

    {:type :wrong}

    {:type :heraldicon.entity.arms/data
     :achievement nil}

    {:type :heraldicon.entity.arms/data
     :achievement :wrong}))
