(ns spec.heraldry.ordinary.geometry-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-geometry
  (are [form] (tu/valid? :heraldry.ordinary/geometry form)
    {}

    {:size-mode (tu/example :heraldry.ordinary.geometry/size-mode)}

    {:size 5}

    {:stretch 1.2}

    {:width 12}

    {:height 10}

    {:thickness 20}

    {:eccentricity 1.4}))

(deftest invalid-geometry
  (are [form] (tu/invalid? :heraldry.ordinary/geometry form)
    {:size-mode :wrong}

    {:size :wrong}

    {:stretch :wrong}

    {:width :wrong}

    {:height :wrong}

    {:thickness :wrong}

    {:eccentricity :wrong}))
