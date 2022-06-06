(ns spec.heraldicon.entity.attribution-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.specs]
   [spec.heraldicon.test-util :as tu]))

(deftest valid-attribution
  (are [form] (tu/valid? :heraldicon.entity/attribution form)
    {}

    {:nature :own-work}

    {:nature :own-work
     :license :cc-attribution}

    {:nature :own-work
     :license-version :v2}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"
     :source-modification "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     ;; :source-license-version missing
     :source-creator-name "foo"
     :source-creator-link "foo"}))

(deftest invalid-attribution
  (are [form] (tu/invalid? :heraldicon.entity/attribution form)
    {:nature :wrong}

    {:nature :own-work
     :license :wrong}

    {:nature :own-work
     :license-version :wrong}

    {:nature :derivative
     :source-name :wrong
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     ;; :source-name  missing
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link :wrong
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     ;; :source-link missing
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :wrong
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     ;; :source-license missing
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :wrong
     :source-creator-name "foo"
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name :wrong
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     ;; :source-creator-name missing
     :source-creator-link "foo"}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link :wrong}

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     ;; :source-creator-link missing
     }

    {:nature :derivative
     :source-name "foo"
     :source-link "foo"
     :source-license :cc-attribution
     :source-license-version :v4
     :source-creator-name "foo"
     :source-creator-link "foo"
     :source-modification :wrong}))
