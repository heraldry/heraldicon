(ns spec.heraldicon.entity.collection.data-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-collection-data
  (are [form] (tu/valid? :heraldicon.entity.collection/data form)
    {:type :heraldicon.entity.collection/data}

    {:type :heraldicon.entity.collection/data
     :num-columns 10}

    {:type :heraldicon.entity.collection/data
     :font :lohengrin}

    {:type :heraldicon.entity.collection/data
     :elements [(tu/example :heraldicon.entity.collection/element)]}))

(deftest invalid-collection-data
  (are [form] (tu/invalid? :heraldicon.entity.collection/data form)
    {}

    {:type :wrong}

    {:type :heraldicon.entity.collection/data
     :num-columns :wrong}

    {:type :heraldicon.entity.collection/data
     :font :wrong}

    {:type :heraldicon.entity.collection/data
     :elements :wrong}

    {:type :heraldicon.entity.collection/data
     :elements [:wrong]}))
