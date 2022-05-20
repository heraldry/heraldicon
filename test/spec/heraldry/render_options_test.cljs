(ns spec.heraldry.render-options-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-render-option
  (are [form] (tu/valid? :heraldry/render-options form)
    {:type :heraldry/render-options}

    {:type :heraldry/render-options
     :escutcheon (tu/example :heraldry.render-options/escutcheon)}

    {:type :heraldry/render-options
     :mode (tu/example :heraldry.render-options/mode)}

    {:type :heraldry/render-options
     :texture (tu/example :heraldry.render-options/texture)}

    {:type :heraldry/render-options
     :texture-displacement? true}

    {:type :heraldry/render-options
     :shiny? true}

    {:type :heraldry/render-options
     :escutcheon-shadow? true}

    {:type :heraldry/render-options
     :escutcheon-outline? true}

    {:type :heraldry/render-options
     :outline? true}

    {:type :heraldry/render-options
     :squiggly true}

    {:type :heraldry/render-options
     :coat-of-arms-angle 42}

    {:type :heraldry/render-options
     :scope (tu/example :heraldry.render-options/scope)}))

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
     :texture-displacement? :wrong}

    {:type :heraldry/render-options
     :shiny? :wrong}

    {:type :heraldry/render-options
     :escutcheon-shadow? :wrong}

    {:type :heraldry/render-options
     :escutcheon-outline? :wrong}

    {:type :heraldry/render-options
     :outline? :wrong}

    {:type :heraldry/render-options
     :squiggly? :wrong}

    {:type :heraldry/render-options
     :coat-of-arms-angle :wrong}

    {:type :heraldry/render-options
     :scope :wrong}))
