(ns spec.heraldry.charge-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(def ^:private example-field
  (tu/example :heraldry/field))

(deftest valid-charge
  (are [form] (tu/valid? :heraldry/charge form)
    {:type :heraldry.charge.type/roundel
     :field example-field}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :anchor (tu/example :heraldry/position)}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :orientation (tu/example :heraldry/position)}

    {:type :heraldry.charge.type/lion
     :field example-field
     :attitude :rampant}

    {:type :heraldry.charge.type/lion
     :field example-field
     :facing :reguardant}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :geometry (tu/example :heraldry.charge/geometry)}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :fimbriation (tu/example :heraldry/fimbriation)}

    {:type :heraldry.charge.type/lion
     :field example-field
     :outline-mode :keep}

    {:type :heraldry.charge.type/lion
     :field example-field
     :vertical-mask 0.6}

    {:type :heraldry.charge.type/lion
     :field example-field
     :escutcheon :heater}

    {:type :heraldry.charge.type/lion
     :field example-field
     :num-points 5}

    {:type :heraldry.charge.type/lion
     :field example-field
     :eccentricity 0.4}

    {:type :heraldry.charge.type/lion
     :field example-field
     :wavy-rays? true}

    {:type :heraldry.charge.type/lion
     :field example-field
     :variant (tu/example :heraldry.charge/variant)}

    {:type :heraldry.charge.type/lion
     :field example-field
     :ignore-layer-separator? true}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :tincture (tu/example :heraldry.charge/tincture)}

    {:type :heraldry.charge.type/lion
     :field example-field
     :manual-blazon "foobar"}))

(deftest invalid-charge
  (are [form] (tu/invalid? :heraldry/charge form)
    {}

    {:type :heraldry.charge.type/wolf
     :field :wrong}

    {:type :heraldry.charge.type/wolf
     :attitude :wrong
     :field example-field}

    {:type :heraldry.charge.type/wolf
     :facing :wrong
     :field example-field}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :geometry :wrong}

    {:type :heraldry.charge.type/roundel
     :field example-field
     :fimbriation :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :outline-mode :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :vertical-mask :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :escutcheon :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :num-points :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :eccentricity :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :wavy-rays? :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :variant :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :ignore-layer-separator? :wrong}

    {:type :heraldry.charge.type/wolf
     :field example-field
     :tincture :wrong}

    {:type :heraldry.charge.type/lion
     :field example-field
     :manual-blazon :wrong}))
