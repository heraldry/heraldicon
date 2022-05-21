(ns spec.heraldry.ribbon.segment-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-charge-tincture
  (are [form] (tu/valid? :heraldry.ribbon/segment form)
    {:type :heraldry.ribbon.segment.type/foreground}

    {:type :heraldry.ribbon.segment.type/foreground
     :offset-x 5}

    {:type :heraldry.ribbon.segment.type/foreground
     :offset-y 5}

    {:type :heraldry.ribbon.segment.type/foreground
     :font-scale 1.2}

    {:type :heraldry.ribbon.segment.type/foreground
     :spacing 3}

    {:type :heraldry.ribbon.segment.type/foreground
     :text "hello"}

    {:type :heraldry.ribbon.segment.type/foreground
     :font :lohengrin}))

(deftest invalid-charge-tincture
  (are [form] (tu/invalid? :heraldry.ribbon/segment form)
    {}

    {:type :heraldry.ribbon.segment.type/foreground
     :offset-x :wrong}

    {:type :heraldry.ribbon.segment.type/foreground
     :offset-y :wrong}

    {:type :heraldry.ribbon.segment.type/foreground
     :font-scale :wrong}

    {:type :heraldry.ribbon.segment.type/foreground
     :spacing :wrong}

    {:type :heraldry.ribbon.segment.type/foreground
     :text :wrong}

    {:type :heraldry.ribbon.segment.type/foreground
     :font :wrong}))
