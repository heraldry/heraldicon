(ns spec.heraldry.position-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-position
  (are [form] (tu/valid? :heraldry/position form)
    {}

    {:point (tu/example :heraldry.position/point)}

    {:offset-x 10}

    {:offset-y 5}

    {:bottom-margin -10}

    {:left-margin 5}

    {:alignment (tu/example :heraldry.position/alignment)}

    {:type (tu/example :heraldry.position/type)}))

(deftest invalid-position
  (are [form] (tu/invalid? :heraldry/position form)
    {:point :wrong}

    {:offset-x :wrong}

    {:offset-y :wrong}

    {:bottom-margin :wrong}

    {:left-margin :wrong}

    {:alignment :wrong}

    {:type :wrong}))
