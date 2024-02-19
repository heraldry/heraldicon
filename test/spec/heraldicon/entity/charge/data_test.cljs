(ns spec.heraldicon.entity.charge.data-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-charge-data
  (are [form] (tu/valid? :heraldicon.entity.charge/data form)
    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {:width 100
                :height 100
                :data [:g]}}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :landscape? true}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :attitude (tu/example :heraldicon.entity.charge.data/attitude)}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :facing (tu/example :heraldicon.entity.charge.data/facing)}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :fixed-tincture (tu/example :heraldicon.entity.charge.data/fixed-tincture)}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :attributes (tu/example :heraldicon.entity.charge.data/attributes)}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :colours (tu/example :heraldicon.entity.charge.data/colours)}))

(deftest invalid-charge-data
  (are [form] (tu/invalid? :heraldicon.entity.charge/data form)
    {}

    {:type :wrong
     :charge-type "foobar"}

    {:type :heraldicon.entity.charge/data
     :charge-type 10}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data :wrong}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {;; :width missing
                :height 100
                :data [:g]}}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {:width :wrong
                :height 100
                :data [:g]}}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {:width 100
                :height :wrong
                :data [:g]}}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {:width 100
                ;; :height missing
                :data [:g]}}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {:width 100
                :height 100
                :data :wrong}}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :edn-data {:width 100
                :height 100
                ;; :data missing
                }}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :landscape? :wrong}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :attitude :wrong}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :facing :wrong}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :fixed-tincture :wrong}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :attributes :wrong}

    {:type :heraldicon.entity.charge/data
     :charge-type "foobar"
     :colours :wrong}))

(deftest valid-charge-data-attributes
  (are [form] (tu/valid? :heraldicon.entity.charge.data/attributes form)
    {}

    {:erased true
     :supporter false}))

(deftest invalid-charge-data-attributes
  (are [form] (tu/invalid? :heraldicon.entity.charge.data/attributes form)
    {:erased :wrong}

    {:wrong true}

    {"foobar" true}))

(deftest valid-charge-data-colours
  (are [form] (tu/valid? :heraldicon.entity.charge.data/colours form)
    {}

    {"#192dfa" :primary}

    {"#000000" :outline}

    {"#00FF00" :shadow}

    {"#192dfa" [:primary :shadow-05]}

    {"#ffFFff" [:tertiary :highlight-25]}))

(deftest invalid-charge-data-colours
  (are [form] (tu/invalid? :heraldicon.entity.charge.data/colours form)
    {:wrong :primary}

    {"wrong" :primary}

    {"#123" :primary}

    {"#192dfa" :wrong}

    {"#192dfa" [:wrong :shadow-05]}

    {"#ffFFff" [:tertiary :wrong]}))
