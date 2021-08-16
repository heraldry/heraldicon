
(ns heraldry.coat-of-arms.default
  (:require [heraldry.config :as config]
            [heraldry.coat-of-arms.vector :as v]))

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
   :field field})

(def render-options
  {:mode :colours
   :outline? false
   :squiggly? false
   :escutcheon-shadow? true
   :ui {:selectable-fields? true}})

(def helmet
  {:type :heraldry.charge.type/helmet
   :function :heraldry.charge.function/helmet
   :variant {:id (config/get :helmet-charge-id)
             :version 0}
   :origin {:point :bottom
            :offset-x -10
            :offset-y 20}
   :geometry {:size 100}
   :field (-> field
              (assoc :tincture :helmet-medium))
   :tincture {:shadow 1
              :highlight 1}})

(def torse
  {:type :heraldry.charge.type/torse
   :function :heraldry.charge.function/torse
   :variant {:id (config/get :torse-charge-id)
             :version 0}
   :origin {:point :fess
            :offset-x 5
            :offset-y -1}
   :geometry {:size 60}
   :field (-> field
              (assoc :tincture :azure))
   :tincture {:shadow 1
              :highlight 1
              :secondary :or}})

(def crest-charge
  {:type :heraldry.charge.type/roundel
   :function :heraldry.charge.function/crest-charge
   :field field
   :tincture {:shadow 1
              :highlight 1}})

(def helm
  {:type :heraldry.component/helm
   :components [helmet]})

(def ribbon
  {:ribbon {:thickness nil
            :points [(v/v -150 -20) (v/v -75 0) (v/v 0 5) (v/v 75 0) (v/v 150 -20)]
            :segments [{:type :heraldry.ribbon.segment/foreground-with-text
                        :text "* LOREM IPSUM *"}]}})

(def motto
  {:type :heraldry.motto.type/motto})

(def slogan
  {:type :heraldry.motto.type/slogan})

