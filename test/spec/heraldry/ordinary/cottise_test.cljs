(ns spec.heraldry.ordinary.cottise-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def ^:private example-field
  (tu/example :heraldry/field))

(deftest valid-cottise
  (are [form] (tu/valid? :heraldry.ordinary/cottise form)
    {:type :heraldry/cottise
     :field example-field}

    {:type :heraldry/cottise
     :field example-field
     :line (tu/example :heraldry/line)}

    {:type :heraldry/cottise
     :field example-field
     :opposite-line (tu/example :heraldry/line)}

    {:type :heraldry/cottise
     :field example-field
     :distance 10}

    {:type :heraldry/cottise
     :field example-field
     :thickness 10}

    {:type :heraldry/cottise
     :field example-field
     :outline? true}))

(deftest invalid-cottise
  (are [form] (tu/invalid? :heraldry.ordinary/cottise form)
    {}

    {:type :heraldry/cottise}

    {:type :heraldry/cottise
     :field :wrong}

    {:type :wrong
     :field example-field}

    {:type :heraldry/cottise
     :field example-field
     :line :wrong}

    {:type :heraldry/cottise
     :field example-field
     :opposite-line :wrong}

    {:type :heraldry/cottise
     :field example-field
     :distance :wrong}

    {:type :heraldry/cottise
     :field example-field
     :thickness :wrong}

    {:type :heraldry/cottise
     :field example-field
     :outline? :wrong}))
