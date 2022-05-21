(ns spec.heraldry.helms-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-helms
  (are [form] (tu/valid? :heraldry/helms form)
    {:type :heraldry/helms}

    {:type :heraldry/helms
     :elements []}

    {:type :heraldry/helms
     :elements [(tu/example :heraldry/helm)]}))

(deftest invalid-helms
  (are [form] (tu/invalid? :heraldry/helms form)
    {}

    {:type :wrong}

    {:type :heraldry/helms
     :elements :wrong}

    {:type :heraldry/helms
     :elements [:wrong]}))
