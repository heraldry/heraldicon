(ns spec.heraldry.ordinary-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def ^:private example-field
  (tu/example :heraldry/field))

(deftest valid-ordinary
  (are [form] (tu/valid? :heraldry/ordinary form)
    {:type :heraldry.ordinary.type/pale
     :field example-field}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :line (tu/example :heraldry/line)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :opposite-line (tu/example :heraldry/line)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :extra-line (tu/example :heraldry/line)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :anchor (tu/example :heraldry/position)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :orientation (tu/example :heraldry/position)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :origin (tu/example :heraldry/position)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :geometry (tu/example :heraldry.ordinary/geometry)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :thickness 10}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :distance 10}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :corner-radius 10}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :num-points 10}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :variant (tu/example :heraldry.ordinary/variant)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :smoothing 10}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :fimbriation (tu/example :heraldry/fimbriation)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :cottising (tu/example :heraldry.ordinary/cottising)}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :manual-blazon "foobar"}))

(deftest invalid-ordinary
  (are [form] (tu/invalid? :heraldry/ordinary form)
    {}

    {:type :heraldry.field.type/per-pale
     :field example-field}

    {:type :does-not-exist
     :field example-field}

    {:type :fess
     :field {}}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :line :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :opposite-line :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :extra-line :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :anchor :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :orientation :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :origin :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :geometry :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :thickness :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :distance :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :corner-radius :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :num-points :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :variant :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :smoothing :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :fimbriation :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :cottising :wrong}

    {:type :heraldry.ordinary.type/fess
     :field example-field
     :manual-blazon :wrong}))
