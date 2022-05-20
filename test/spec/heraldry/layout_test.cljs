(ns spec.heraldry.layout-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-layout
  (are [form] (tu/valid? :heraldry/layout form)
    {}

    {:num-base-fields 3}

    {:num-fields-x 6}

    {:num-fields-y 7}

    {:offset-x 3}

    {:offset-y 15}

    {:stretch-x 1.2}

    {:stretch-y 0.4}

    {:rotation 30}))

(deftest invalid-layout
  (are [form] (tu/invalid? :heraldry/layout form)
    {:num-base-fields :wrong}

    {:num-fields-x :wrong}

    {:num-fields-y :wrong}

    {:offset-x :wrong}

    {:offset-y :wrong}

    {:stretch-x :wrong}

    {:stretch-y :wrong}

    {:rotation :wrong}))
