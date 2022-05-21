(ns spec.heraldicon.entity.ribbon.data-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-ribbon-data
  (are [form] (tu/valid? :heraldicon.entity.ribbon/data form)
    {:type :heraldicon.entity.ribbon/data
     :ribbon (tu/example :heraldry/ribbon)}))

(deftest invalid-ribbon-data
  (are [form] (tu/invalid? :heraldicon.entity.ribbon/data form)
    {}

    {:type :wrong}

    {:type :heraldicon.entity.ribbon/data
     :ribbon nil}

    {:type :heraldicon.entity.ribbon/data
     :ribbon :wrong}))
