(ns spec.heraldicon.entity-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-entity
  (are [form] (tu/valid? :heraldicon/entity form)
    {:type :heraldicon.entity.type/arms
     :name "foobar"}

    {:type :heraldicon.entity.type/charge
     :name "foobar"}

    {:type :heraldicon.entity.type/ribbon
     :name "foobar"}

    {:type :heraldicon.entity.type/collection
     :name "foobar"}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :access :private}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :tags {}}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :tags {:foo true
            :bar false}}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :attribution {}}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :attribution (tu/example :heraldicon.entity/attribution)}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :metadata []}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :metadata (tu/example :heraldicon.entity/metadata)}))

(deftest invalid-entity
  (are [form] (tu/invalid? :heraldicon/entity form)
    {}

    {:type :heraldicon.entity.type/foo
     :name "foobar"}

    {:type :heraldicon.entity.type/arms}

    {:type :heraldicon.entity.type/arms
     :name ""}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :access nil}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :access :wrong}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :tags {:foo :bar}}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :tags {"foo" true}}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :attribution :foobar}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :metadata :foobar}

    {:type :heraldicon.entity.type/arms
     :name "foobar"
     :metadata [[:foo 5]]}))
