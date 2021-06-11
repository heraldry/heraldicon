
(ns heraldry.coat-of-arms.default
  (:require [heraldry.config :as config]))

(def field
  {:type :heraldry.field.type/plain
   :tincture :none})

(def ordinary
  {:type :heraldry.ordinary.type/pale
   :line {:type :straight}
   :field field
   :hints {:outline? true}})

(def charge
  {:type :heraldry.charge.type/roundel
   :field field
   :tincture {:shadow 1
              :highlight 1}
   :hints {:outline-mode :keep}})

(def charge-group
  {:type :heraldry.charge-group.type/rows
   :strip-angle 0
   :spacing 20
   :stretch 0.866
   :charges [charge]
   :strips [{:size 2
             :slots [0 0]}
            {:size 1
             :slots [0]}]})

(def charge-group-strip
  {:size 3
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
                       :highlight 1}
            :hints {:outline-mode :keep}}})

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
