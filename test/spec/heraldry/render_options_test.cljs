(ns spec.heraldry.render-options-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-render-option
  (are [form] (tu/valid? :heraldry/render-options form)
    nil

    {:type :heraldry/render-options}

    {:type :heraldry/render-options
     :escutcheon nil}

    {:type :heraldry/render-options
     :escutcheon :heater}

    {:type :heraldry/render-options
     :mode nil}

    {:type :heraldry/render-options
     :mode :colours}

    {:type :heraldry/render-options
     :texture nil}

    {:type :heraldry/render-options
     :texture :felt}

    {:type :heraldry/render-options
     :texture-displacement? nil}

    {:type :heraldry/render-options
     :texture-displacement? true}

    {:type :heraldry/render-options
     :shiny? nil}

    {:type :heraldry/render-options
     :shiny? true}

    {:type :heraldry/render-options
     :escutcheon-shadow? nil}

    {:type :heraldry/render-options
     :escutcheon-shadow? true}

    {:type :heraldry/render-options
     :escutcheon-outline? nil}

    {:type :heraldry/render-options
     :escutcheon-outline? true}

    {:type :heraldry/render-options
     :outline? nil}

    {:type :heraldry/render-options
     :outline? true}

    {:type :heraldry/render-options
     :squiggly nil}

    {:type :heraldry/render-options
     :squiggly true}

    {:type :heraldry/render-options
     :coat-of-arms-angle nil}

    {:type :heraldry/render-options
     :coat-of-arms-angle 42}

    {:type :heraldry/render-options
     :scope nil}

    {:type :heraldry/render-options
     :scope :achievement}))

(deftest invalid-render-option
  (are [form] (tu/invalid? :heraldry/render-options form)
    {}

    {:type :heraldry/render-options
     :escutcheon :wrong}

    {:type :heraldry/render-options
     :mode :wrong}

    {:type :heraldry/render-options
     :texture :wrong}

    {:type :heraldry/render-options
     :texture-displacement? 42}

    {:type :heraldry/render-options
     :shiny? 5.6}

    {:type :heraldry/render-options
     :escutcheon-shadow? :foobar}

    {:type :heraldry/render-options
     :escutcheon-outline? "foo"}

    {:type :heraldry/render-options
     :outline? 5}

    {:type :heraldry/render-options
     :squiggly? "foo"}

    {:type :heraldry/render-options
     :coat-of-arms-angle true}

    {:type :heraldry/render-options
     :scope 0}))
