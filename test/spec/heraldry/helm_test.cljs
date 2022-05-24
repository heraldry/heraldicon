(ns spec.heraldry.helm-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-helm
  (are [form] (tu/valid? :heraldry/helm form)
    {:type :heraldry/helm}

    {:type :heraldry/helm
     :elements []}

    {:type :heraldry/helm
     :components [(tu/example :heraldry.helm/component)]}))

(deftest invalid-helm
  (are [form] (tu/invalid? :heraldry/helm form)
    {}

    {:type :wrong}

    {:type :heraldry/helm
     :components :wrong}

    {:type :heraldry/helm
     :components [:wrong]}))

(deftest valid-helm-component
  (are [form] (tu/valid? :heraldry.helm/component form)
    (tu/example :heraldry/charge)

    (tu/example :heraldry/charge-group)

    (tu/example :heraldry/shield-separator)))

(deftest invalid-helm-component
  (are [form] (tu/invalid? :heraldry.helm/component form)
    {}

    :wrong))
