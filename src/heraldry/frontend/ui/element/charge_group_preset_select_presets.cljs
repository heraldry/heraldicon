(ns heraldry.frontend.ui.element.charge-group-preset-select-presets)

(def presets
  [[:string.charge-group.presets.group/ordinaries
    [:string.charge-group.presets/fesswise :fesswise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [:string.charge-group.presets/palewise :palewise
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 20
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [:string.charge-group.presets/bendwise :bendwise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [:string.charge-group.presets/bendwise-sinister :bendwise-sinister
     {:type :heraldry.charge-group.type/rows
      :strip-angle -45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:type :heraldry.component/charge-group-strip
                :slots [0 0 0]}]}]
    [:string.charge-group.presets/chevronwise :chevronwise
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
    [:string.charge-group.presets/in-cross :in-cross
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
    [:string.charge-group.presets/in-saltire :in-saltire
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
    [:string.charge-group.presets/in-pall :in-pall
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
    [:string.charge-group.presets/three-above-bend :three-in-above-bend
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
    [:string.charge-group.presets/three-below-bend :three-below-bend
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
   [:string.charge-group.presets.group/triangular
    [:string.charge-group.presets/three :three
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
    [:string.charge-group.presets/three-inverted :three-inverted
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

    [:string.charge-group.presets/three-columns :three-columns
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
    [:string.charge-group.presets/three-columns-inverted :three-columns-inverted
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
    [:string.charge-group.presets/six :six
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
   [:string.charge-group.presets.group/grid
    [:string.charge-group.presets/square :square
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
    [:string.charge-group.presets/diamond :diamond
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
    [:string.charge-group.presets/semy-rows :semy-rows
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
    [:string.charge-group.presets/semy-columns :semy-columns
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
    [:string.charge-group.presets/frame :frame
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
   [:string.charge-group.presets.group/arc
    [:string.charge-group.presets/in-annullo :in-annullo
     {:type :heraldry.charge-group.type/arc
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0 0 0]}]
    [:string.charge-group.presets/arc :arc
     {:type :heraldry.charge-group.type/arc
      :start-angle 10
      :arc-angle 180
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0]}]
    [:string.charge-group.presets/in-annullo-point-to-center :arc-point-to-center
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
    [:string.charge-group.presets/in-annullo-follow :in-annullo-follow
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
   [:string.charge-group.presets.group/others
    [:string.charge-group.presets/in-orle :in-orle
     {:type :heraldry.charge-group.type/in-orle
      :distance 10
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 10}}]
      :slots [0 0 0 0 0 0 0 0]}
     {}]
    [:string.charge-group.presets/sheaf-of :sheaf-of
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
