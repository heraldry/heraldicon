
(ns heraldry.coat-of-arms.default
  (:require [heraldry.config :as config]))

(def line
  {:type :straight})

(def field
  {:type :heraldry.field.type/plain
   :tincture :none})

(def ordinary
  {:type :heraldry.ordinary.type/pale
   :line line
   :field field
   :outline? true})

(def cottise
  {:field field
   :enabled? true})

(def charge
  {:type :heraldry.charge.type/roundel
   :field field
   :tincture {:shadow 1
              :highlight 1}})

(def charge-group
  {:type :heraldry.charge-group.type/rows
   :strip-angle 0
   :spacing 40
   :stretch 0.866
   :charges [charge]
   :strips [{:type :heraldry.component/charge-group-strip
             :slots [0 0]}
            {:type :heraldry.component/charge-group-strip
             :slots [0]}]})

(def charge-group-strip
  {:type :heraldry.component/charge-group-strip
   :slots [0 0 0]})

(def semy
  {:type :heraldry.component/semy
   :layout {:num-fields-x 6}
   :charge {:type :heraldry.charge.type/fleur-de-lis
            :variant {:id (config/get :fleur-de-lis-charge-id)
                      :version 0}
            :field {:type :heraldry.field.type/plain
                    :tincture :or}
            :tincture {:shadow 1
                       :highlight 1}}})

(def coat-of-arms
  {:spec-version 1
   :escutcheon :heater
   :field field})

(def render-options
  {:mode :colours
   :outline? false
   :squiggly? false
   :escutcheon-shadow? true
   :ui {:selectable-fields? true}})

(def helm
  {:type :heraldry.component/helm})

(def helmet
  {:type :heraldry.component/helmet})

(def torse
  {:type :heraldry.component/torse})

(def crest
  {:type :heraldry.component/crest})
