(ns spec.heraldry.line-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-line
  (are [form] (tu/valid? :heraldry/line form)
    {}

    {:type (tu/example :heraldry.line/type)}

    {:eccentricity 1.25}

    {:width 10}

    {:offset 0.5}

    {:spacing 1.6}

    {:base-line (tu/example :heraldry.line/base-line)}

    {:corner-damping-radius 13}

    {:corner-damping-mode (tu/example :heraldry.line/corner-damping-mode)}

    {:flipped? true}

    {:mirrored? false}

    {:fimbriation (tu/example :heraldry/fimbriation)}

    {:size-reference (tu/example :heraldry.line/size-reference)}))

(deftest invalid-line
  (are [form] (tu/invalid? :heraldry/line form)
    {:type :wrong}

    {:eccentricity :wrong}

    {:width :wrong}

    {:offset :wrong}

    {:spacing :wrong}

    {:base-line :wrong}

    {:corner-damping-radius :wrong}

    {:corner-damping-mode :wrong}

    {:flipped? :wrong}

    {:mirrored? :wrong}

    {:fimbriation :wrong}

    {:size-reference :wrong}))
