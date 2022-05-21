(ns spec.heraldry.ribbon-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [heraldicon.math.vector :as v]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-charge-tincture
  (are [form] (tu/valid? :heraldry/ribbon form)
    {:type :heraldry/ribbon}

    {:type :heraldry/ribbon
     :thickness 12}

    {:type :heraldry/ribbon
     :edge-angle 33}

    {:type :heraldry/ribbon
     :end-split 10}

    {:type :heraldry/ribbon
     :outline? true}

    {:type :heraldry/ribbon
     :points []}

    {:type :heraldry/ribbon
     :points [v/zero]}

    {:type :heraldry/ribbon
     :segments []}

    {:type :heraldry/ribbon
     :segments [(tu/example :heraldry.ribbon/segment)]}))

(deftest invalid-charge-tincture
  (are [form] (tu/invalid? :heraldry/ribbon form)
    {}

    {:type :wrong}

    {:type :heraldry/ribbon
     :thickness :wrong}

    {:type :heraldry/ribbon
     :edge-angle :wrong}

    {:type :heraldry/ribbon
     :end-split :wrong}

    {:type :heraldry/ribbon
     :outline? :wrong}

    {:type :heraldry/ribbon
     :points :wrong}

    {:type :heraldry/ribbon
     :points [:wrong]}

    {:type :heraldry/ribbon
     :segments :wrong}

    {:type :heraldry/ribbon
     :segments [:wrong]}))
