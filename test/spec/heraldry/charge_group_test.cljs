(ns spec.heraldry.charge-group-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def example-charge
  (tu/example :heraldry/charge))

(deftest valid-charge-group
  (are [form] (tu/valid? :heraldry/charge-group form)
    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :manual-blazon "foobar"}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :anchor (tu/example :heraldry.charge-group/anchor)}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :spacing 5}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :stretch 1.1}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :strip-angle 25}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :strips [(tu/example :heraldry.charge-group/strip)]}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :anchor (tu/example :heraldry.charge-group/anchor)}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :start-angle 45}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :arc-angle 180}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :slots [0 0 nil]}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :radius 75}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :arc-stretch 1.3}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :rotate-charges? true}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :distance 10}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :offset 0.45}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :slots [0]}))

(deftest invalid-charge-group
  (are [form] (tu/invalid? :heraldry/charge-group form)
    {}

    {:type :heraldry.charge-group.type/rows}

    {:charges [example-charge]}

    {:type :heraldry.charge-group.type/rows
     :charges :wrong}

    {:type :wrong
     :charges [example-charge]}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :manual-blazon :wrong}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :anchor :wrong}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :spacing :wrong}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :stretch :wrong}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :strip-angle :wrong}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :strips :wrong}

    {:type :heraldry.charge-group.type/rows
     :charges [example-charge]
     :strips [:wrong]}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :anchor :wrong}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :start-angle :wrong}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :arc-angle :wrong}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :slots :wrong}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :slots [:wrong]}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :radius :wrong}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :arc-stretch :wrong}

    {:type :heraldry.charge-group.type/arc
     :charges [example-charge]
     :rotate-charges? :wrong}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :distance :wrong}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :offset :wrong}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :slots :wrong}

    {:type :heraldry.charge-group.type/in-orle
     :charges [example-charge]
     :slots [:wrong]}))

(deftest valid-charge-group-strip
  (are [form] (tu/valid? :heraldry.charge-group/strip form)
    {:type :heraldry.charge-group.element.type/strip}

    {:type :heraldry.charge-group.element.type/strip
     :slots [0 nil 1]}

    {:type :heraldry.charge-group.element.type/strip
     :stretch 0.7}

    {:type :heraldry.charge-group.element.type/strip
     :offset 0.3333}))

(deftest invalid-charge-group-strip
  (are [form] (tu/invalid? :heraldry.charge-group/strip form)
    {}

    {:type :wrong}

    {:type :heraldry.charge-group.element.type/strip
     :slots :wrong}

    {:type :heraldry.charge-group.element.type/strip
     :slots [:wrong]}

    {:type :heraldry.charge-group.element.type/strip
     :stretch :wrong}

    {:type :heraldry.charge-group.element.type/strip
     :offset :wrong}))
