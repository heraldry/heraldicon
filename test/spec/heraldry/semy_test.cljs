(ns spec.heraldry.semy-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def example-charge
  (tu/example :heraldry/charge))

(deftest valid-semy
  (are [form] (tu/valid? :heraldry/semy form)
    {:type :heraldry/semy
     :charge example-charge}

    {:type :heraldry/semy
     :charge example-charge
     :layout (tu/example :heraldry.semy/layout)}

    {:type :heraldry/semy
     :charge example-charge
     :rectangular? true}

    {:type :heraldry/semy
     :charge example-charge
     :manual-blazon "foobar"}))

(deftest invalid-semy
  (are [form] (tu/invalid? :heraldry/semy form)
    {}

    {:type :heraldry/semy}

    {:charge example-charge}

    {:type :heraldry/semy
     :charge :wrong}

    {:type :wrong
     :charge example-charge}

    {:type :heraldry/semy
     :charge example-charge
     :layout :wrong}

    {:type :heraldry/semy
     :charge example-charge
     :rectangular? :wrong}

    {:type :heraldry/semy
     :charge example-charge
     :manual-blazon :wrong}))
