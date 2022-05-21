(ns spec.heraldry.motto-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.specs]))

(deftest valid-charge-tincture
  (are [form] (tu/valid? :heraldry/motto form)
    {:type :heraldry.motto.type/motto}

    {:type :heraldry.motto.type/slogan
     :anchor (tu/example :heraldry/position)}

    {:type :heraldry.motto.type/motto
     :tincture-foreground :or}

    {:type :heraldry.motto.type/motto
     :tincture-background :sable}

    {:type :heraldry.motto.type/motto
     :tincture-background :none}

    {:type :heraldry.motto.type/motto
     :tincture-text :vert}

    {:type :heraldry.motto.type/motto
     :ribbon (tu/example :heraldry/ribbon)}))

(deftest invalid-charge-tincture
  (are [form] (tu/invalid? :heraldry/motto form)
    {}

    {:type :wrong}

    {:type :heraldry.motto.type/slogan
     :anchor :wrong}

    {:type :heraldry.motto.type/motto
     :tincture-foreground :wrong}

    {:type :heraldry.motto.type/motto
     :tincture-background :wrong}

    {:type :heraldry.motto.type/motto
     :tincture-background :wrong}

    {:type :heraldry.motto.type/motto
     :tincture-text :wrong}

    {:type :heraldry.motto.type/motto
     :ribbon :wrong}))
