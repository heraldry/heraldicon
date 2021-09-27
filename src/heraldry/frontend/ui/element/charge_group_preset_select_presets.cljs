(ns heraldry.frontend.ui.element.charge-group-preset-select-presets)

(def presets
  [[{:en "Ordinaries"
     :de "Heroldsbilder"}
    [{:en "Fesswise"
      :de "Balkenweise"} :fesswise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [{:en "Palewise"
      :de "Pfahlweise"} :palewise
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 20
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [{:en "Bendwise"
      :de "Schrägbalkenweise"} :bendwise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [{:en "Bendwise (sinister)"
      :de "Schräglinksbalenweise"} :bendwise-sinister
     {:type :heraldry.charge-group.type/rows
      :strip-angle -45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [{:en "Chevronwise"
      :de "Sparrenweise"} :chevronwise
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
    [{:en "In cross"
      :de "Kreuzweise"} :in-cross
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
    [{:en "In saltire"
      :de "Schräggekreuzt"} :in-saltire
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
    [{:en "In pall"
      :de "Deichselweise"} :in-pall
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
    [{:en "Three above bend"
      :de "Drei über Schrägbalken"} :three-in-above-bend
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
    [{:en "Three below bend"
      :de "Drei unter Schrägbalken"} :three-below-bend
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
   [{:en "Triangular"
     :de "Dreieckig"}
    [{:en "Three"
      :de "Drei"} :three
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
    [{:en "Three (inverted)"
      :de "Drei (invertiert)"} :three-inverted
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

    [{:en "Three (columns)"
      :de "Drei (Spalten)"} :three-columns
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
    [{:en "Three (columns, inverted)"
      :de "Drei (Spalten, invertiert)"} :three-columns-inverted
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
    [{:en "Six"
      :de "Sechs"} :six
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
   [{:en "Grid"
     :de "Gitter"}
    [{:en "Square"
      :de "Quadrat"} :square
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
    [{:en "Diamond"
      :de "Diamant"} :diamond
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
    [{:en "Semy rows"
      :de "Gesäte Reihen"} :semy-rows
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
    [{:en "Semy columns"
      :de "Gesäte Spalten"} :semy-columns
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
    [{:en "Frame"
      :de "Rahmen"} :frame
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
   [{:en "Arc"
     :de "Bogen"}
    [{:en "In annullo"
      :de "Kreis"} :in-annullo
     {:type :heraldry.charge-group.type/arc
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0 0 0]}]
    [{:en "Arc"
      :de "Bogen"} :arc
     {:type :heraldry.charge-group.type/arc
      :start-angle 10
      :arc-angle 180
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0]}]
    [{:en "In annullo (point to center)"
      :de "Kreis (Richtung Mittelpunkt)"} :arc-point-to-center
     {:type :heraldry.charge-group.type/arc
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :anchor {:point :angle
                          :angle -180}}]
      :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] -180}]
    [{:en "In annullo (follow)"
      :de "Kreis (folgend)"} :in-annullo-follow
     {:type :heraldry.charge-group.type/arc
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :anchor {:point :angle
                          :angle -90}}]
      :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] -90}]
    [{:en "Sheaf of"
      :de "Garbenweise"} :sheaf-of
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
