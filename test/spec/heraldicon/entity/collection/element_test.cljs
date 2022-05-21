(ns spec.heraldicon.entity.collection.element-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-collection-element
  (are [form] (tu/valid? :heraldicon.entity.collection/element form)
    {:type :heraldicon.entity.collection/element}

    {:type :heraldicon.entity.collection/element
     :name "foobar"}

    {:type :heraldicon.entity.collection/element
     :reference {:id "some-id"
                 :version 14}}))

(deftest invalid-collection-element
  (are [form] (tu/invalid? :heraldicon.entity.collection/element form)
    {}

    {:type :wrong}

    {:type :heraldicon.entity.collection/element
     :name :wrong}

    {:type :heraldicon.entity.collection/element
     :reference :wrong}

    {:type :heraldicon.entity.collection/element
     :reference {:id :wrong
                 :version 14}}

    {:type :heraldicon.entity.collection/element
     :reference {:id "some-id"
                 :version :wrong}}))
