(ns heraldry.frontend.ui.element.charge-group-preset-select-presets
  (:require [heraldry.gettext :refer [string]]))

(def presets
  [[(string "Ordinaries")
    [(string "Fesswise") :fesswise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [(string "Palewise") :palewise
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 20
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [(string "Bendwise") :bendwise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [(string "Bendwise (sinister)") :bendwise-sinister
     {:type :heraldry.charge-group.type/rows
      :strip-angle -45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [(string "Chevronwise") :chevronwise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 30
      :stretch 0.6
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 nil 0]}]}]
    [(string "In cross") :in-cross
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 20
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}]}]
    [(string "In saltire") :in-saltire
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 20
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 nil nil]}]}]
    [(string "In pall") :in-pall
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 14
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 18}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 nil nil nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil 0 nil nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil nil 0 0 0]
                :stretch 1.25}
               {:type :heraldry.component/charge-group-strip
                :slots [nil 0 nil nil nil]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 nil nil nil nil]}]}]
    [(string "Three above bend") :three-in-above-bend
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 30
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 20}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [nil 0]}]}]
    [(string "Three below bend") :three-below-bend
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 50
      :stretch 0.3
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 20}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0]}]}]]
   [(string "Triangular")
    [(string "Three") :three
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 40
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0]}]}]
    [(string "Three (inverted)") :three-inverted
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 40
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0]}]}]

    [(string "Three (columns)") :three-columns
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 30
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0]}]}]
    [(string "Three (columns, inverted)") :three-columns-inverted
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 30
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0]}]}]
    [(string "Six") :six
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 30
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0]}]}]]
   [(string "Grid")
    [(string "Square") :square
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 25
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [(string "Diamond") :diamond
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 25
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [(string "Semy rows") :semy-rows
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 20
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0]}]}]
    [(string "Semy columns") :semy-columns
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 17
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0]}]}]
    [(string "Frame") :frame
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 17
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 nil nil nil 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 nil nil nil 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 nil nil nil 0]}
               {:type :heraldry.component/charge-group-strip
                :slots [0 0 0 0 0]}]}]]
   [(string "Arc")
    [(string "In annullo") :in-annullo
     {:type :heraldry.charge-group.type/arc
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0 0 0]}]
    [(string "Arc") :arc
     {:type :heraldry.charge-group.type/arc
      :start-angle 10
      :arc-angle 180
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0]}]
    [(string "In annullo (point to center)") :arc-point-to-center
     {:type :heraldry.charge-group.type/arc
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :anchor {:point :angle
                          :angle 180}}]
      :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] 180}]
    [(string "In annullo (follow)") :in-annullo-follow
     {:type :heraldry.charge-group.type/arc
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :anchor {:point :angle
                          :angle 90}}]
      :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] 90}]
    ]
   [(string "Others")
    [(string "In orle") :in-orle
     {:type :heraldry.charge-group.type/in-orle
      :distance 10
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 10}}]
      :slots [0 0 0 0 0 0 0 0]}
     {}]
    [(string "Sheaf of") :sheaf-of
     {:type :heraldry.charge-group.type/arc
      :start-angle -45
      :arc-angle 90
      :radius 0
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 50
                            :stretch 3}}]
      :slots [0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] 0
      [:charges 0 :geometry :size] 50}]]])
