
(ns heraldicon.heraldry.default
  (:require
   [heraldicon.config :as config]
   [heraldicon.math.vector :as v]))

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
  {:field {:type :heraldry.field.type/plain
           :tincture :or}})

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
   :anchor {:point :bottom
            :offset-x 0
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
   :anchor {:point :fess
            :offset-x 15
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

(def shield-separator
  {:type :heraldry.component/shield-separator})

(def helm
  {:type :heraldry.component/helm
   :components [shield-separator
                helmet]})

(def ribbon
  {:ribbon {:thickness nil
            :points [(v/Vector. -150 -20) (v/Vector. -75 0) (v/Vector. 0 5) (v/Vector. 75 0) (v/Vector. 150 -20)]
            :segments [{:type :heraldry.ribbon.segment/foreground-with-text
                        :text "LOREM IPSUM"}]}})
;;
;; TODO: fetch a default ribbon
(def motto
  {:type :heraldry.motto.type/motto})

(def slogan
  {:type :heraldry.motto.type/slogan})

(def mantling-charge
  {:type :heraldry.charge.type/mantling
   :variant {:id (config/get :mantling-charge-id)
             :version 0}
   :field field
   :tincture {:shadow 1
              :highlight 1
              :secondary :or}
   :anchor {:point :honour}
   :orientation {:point :angle
                 :angle 0}
   :geometry {:size 200}})

(def compartment-charge
  {:type :heraldry.charge.type/compartment
   :variant {:id (config/get :compartment-charge-id)
             :version 0}
   :field {:type :heraldry.field.type/plain
           :tincture :vert}
   :tincture {:shadow 1
              :highlight 1}
   :anchor {:point :bottom}
   :orientation {:point :angle
                 :angle 0}
   :geometry {:size 270}})

(def supporter-left-charge
  {:type :heraldry.charge.type/supporter
   :variant {:id (config/get :supporter-charge-id)
             :version 0}
   :field field
   :tincture {:shadow 1
              :highlight 1}
   :anchor {:point :left
            :offset-x -25
            :offset-y 10}
   :orientation {:point :angle
                 :angle 0}
   :geometry {:size 150
              :mirrored? true}})

(def supporter-right-charge
  {:type :heraldry.charge.type/supporter
   :variant {:id (config/get :supporter-charge-id)
             :version 0}
   :field field
   :tincture {:shadow 1
              :highlight 1}
   :anchor {:point :right
            :offset-x 25
            :offset-y 10}
   :orientation {:point :angle
                 :angle 0}
   :geometry {:size 150}})

(def ornament-charge
  {:type :heraldry.charge.type/roundel
   :field field
   :anchor {:point :bottom}
   :geometry {:size 30}
   :tincture {:shadow 1
              :highlight 1}})

(def ornament-charge-group
  {:type :heraldry.charge-group.type/rows
   :anchor {:point :bottom}
   :strip-angle 0
   :spacing 40
   :stretch 0.866
   :charges [ornament-charge]
   :strips [{:type :heraldry.component/charge-group-strip
             :slots [0 0]}
            {:type :heraldry.component/charge-group-strip
             :slots [0]}]})
