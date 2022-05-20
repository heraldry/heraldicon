(ns spec.heraldry.fimbriation-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-fimbriation
  (are [form] (tu/valid? :heraldry/fimbriation form)
    {}

    {:mode (tu/example :heraldry.fimbriation/mode)}

    {:alignment (tu/example :heraldry.fimbriation/alignment)}

    {:corner (tu/example :heraldry.fimbriation/corner)}

    {:thickness-1 10}

    {:thickness-2 5}

    {:tincture-1 :or}

    {:tincture-2 :none}))

(deftest invalid-fimbriation
  (are [form] (tu/invalid? :heraldry/fimbriation form)
    {:mode :wrong}

    {:alignment :wrong}

    {:corner :wrong}

    {:thickness-1 :wrong}

    {:thickness-2 :wrong}

    {:tincture-1 :wrong}

    {:tincture-2 :wrong}))
