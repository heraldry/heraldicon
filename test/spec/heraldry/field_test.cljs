(ns spec.heraldry.field-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-field
  (are [form] (tu/valid? :heraldry/field form)
    {:type :heraldry.field.type/plain
     :tincture :azure}

    {:type :heraldry.field.type/quartered
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}]}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :components [(tu/example :heraldry/ordinary)
                  (tu/example :heraldry/charge)
                  (tu/example :heraldry/charge-group)
                  (tu/example :heraldry/semy)]}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :line (tu/example :heraldry/line)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :opposite-line (tu/example :heraldry/line)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :extra-line (tu/example :heraldry/line)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :layout (tu/example :heraldry/layout)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :anchor (tu/example :heraldry/position)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :orientation (tu/example :heraldry/position)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :origin (tu/example :heraldry/position)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :outline? true}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :geometry (tu/example :heraldry.field/geometry)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :thickness 50}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :gap 10}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :variant (tu/example :heraldry.field/variant)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :fess-group (tu/example :heraldry.field/fess-group)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :pale-group (tu/example :heraldry.field/pale-group)}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :manual-blazon "foobar"}))

(deftest invalid-field
  (are [form] (tu/invalid? :heraldry/field form)
    {}

    {:type :heraldry.field.type/wrong}

    {:type :heraldry.charge.type/roundel
     :tincture :or}

    {:type :heraldry.field.type/per-pale
     :tincture :azure}

    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/not-valid
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}]}

    {:type :heraldry.field.type/per-saltire
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/reference
               :not-index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}]}

    {:type :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :components :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :components [:wrong]}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :inherit-environment? :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :line :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :opposite-line :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :extra-line :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :layout :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :anchor :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :orientation :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :origin :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :outline? :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :geometry :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :thickness :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :gap :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :variant :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :fess-group :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :pale-group :wrong}

    {:type :heraldry.field.type/plain
     :tincture :azure
     :manual-blazon :wrong}))
