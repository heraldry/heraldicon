(ns heraldry.coat-of-arms.field.options
  (:require [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]))

(def default-options
  {:type {:type :choice
          ;; TODO: this should have the proper choices, but currently that's a circular dependency
          :choices []
          :ui {:label "Division"
               :form-type :field-type-select}}
   :inherit-environment? {:type :boolean
                          :default false
                          :ui {:label "Inherit environment (dimidiation)"}}
   :counterchanged? {:type :boolean
                     :default false
                     :ui {:label "Counterchanged"}}
   :tincture {:type :choice
              :choices tincture/choices
              :default :none
              :ui {:label "Tincture"
                   :form-type :tincture-select}}
   :line (-> line/default-options
             (assoc-in [:ui :label] "Line"))
   :opposite-line (-> line/default-options
                      (assoc-in [:ui :label] "Opposite line"))
   :extra-line (-> line/default-options
                   (assoc-in [:ui :label] "Extra line"))
   :origin (-> position/default-options
               (dissoc :alignment)
               (assoc :ui {:label "Origin"
                           :form-type :position}))
   :direction-anchor (-> position/anchor-default-options
                         (dissoc :alignment)
                         (assoc-in [:angle :min] -180)
                         (assoc-in [:angle :max] 180)
                         (assoc-in [:angle :default] 0)
                         (assoc :ui {:label "Issuant"
                                     :form-type :position}))
   :anchor (-> position/anchor-default-options
               (dissoc :alignment)
               (assoc :ui {:label "Anchor"
                           :form-type :position}))
   :variant {:type :choice
             :choices [["Default" :default]
                       ["Counter" :counter]
                       ["In pale" :in-pale]
                       ["En point" :en-point]
                       ["Ancien" :ancien]]
             :default :default
             :ui {:label "Variant"}}
   :thickness {:type :range
               :min 0
               :max 0.5
               :default 0.1
               :ui {:label "Thickness"
                    :step 0.01}}
   :layout {:num-fields-x {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label "x-Subfields"
                                :form-type :field-layout-num-fields-x}}
            :num-fields-y {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label "y-Subfields"
                                :form-type :field-layout-num-fields-y}}
            :num-base-fields {:type :range
                              :min 2
                              :max 8
                              :default 2
                              :integer? true
                              :ui {:label "Base fields"
                                   :form-type :field-layout-num-base-fields}}
            :offset-x {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label "Offset x"
                            :step 0.01}}
            :offset-y {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label "Offset y"
                            :step 0.01}}
            :stretch-x {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label "Stretch x"
                             :step 0.01}}
            :stretch-y {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label "Stretch y"
                             :step 0.01}}
            :rotation {:type :range
                       :min -90
                       :max 90
                       :default 0
                       :ui {:label "Rotation"
                            :step 0.01}}
            :ui {:label "Layout"
                 :form-type :field-layout}}
   :outline? {:type :boolean
              :default false
              :ui {:label "Outline"}}})

(defn options [field]
  (when field
    (let [line-style (-> (line/options (:line field))
                         (assoc :ui (-> default-options :line :ui)))
          sanitized-line (options/sanitize (:line field) line-style)
          opposite-line-style (-> (line/options (:opposite-line field) :inherited sanitized-line)
                                  (dissoc :fimbriation)
                                  (assoc :ui (-> default-options :opposite-line :ui)))
          extra-line-style (-> (line/options (:extra-line field) :inherited sanitized-line)
                               (dissoc :fimbriation)
                               (-> default-options :extra-line :ui))]
      (-> (case (-> field :type name keyword)
            :plain (options/pick default-options
                                 [[:type]
                                  [:inherit-environment?]
                                  [:counterchanged?]
                                  [:tincture]])
            :per-pale (options/pick default-options
                                    [[:type]
                                     [:inherit-environment?]
                                     [:counterchanged?]
                                     [:line]
                                     [:origin :point]
                                     [:origin :offset-x]
                                     [:outline?]]
                                    {[:origin :point :choices] position/point-choices-x
                                     [:line] line-style})
            :per-fess (options/pick default-options
                                    [[:type]
                                     [:inherit-environment?]
                                     [:counterchanged?]
                                     [:line]
                                     [:origin :point]
                                     [:origin :offset-y]
                                     [:outline?]]
                                    {[:origin :point :choices] position/point-choices-y
                                     [:line] line-style})
            :per-bend (options/pick default-options
                                    [[:type]
                                     [:inherit-environment?]
                                     [:counterchanged?]
                                     [:line]
                                     [:origin]
                                     [:anchor]
                                     [:outline?]]
                                    (let [useful-points #{:top-left :bottom-right
                                                          :chief :honour :fess :nombril :base}
                                          point-choices (util/filter-choices
                                                         position/anchor-point-choices
                                                         useful-points)
                                          anchor-point-choices (util/filter-choices
                                                                position/anchor-point-choices
                                                                (conj useful-points :angle))]
                                      {[:line] line-style
                                       [:origin :point :choices] point-choices
                                       [:origin :point :default] :top-left
                                       [:anchor :point :choices] (case (-> field :origin :point (or :top-left))
                                                                   :top-left (util/filter-choices
                                                                              anchor-point-choices
                                                                              #{:bottom-right
                                                                                :chief :honour :fess :nombril :base :angle})
                                                                   :bottom-right (util/filter-choices
                                                                                  anchor-point-choices
                                                                                  #{:top-left
                                                                                    :chief :honour :fess :nombril :base :angle})
                                                                   (util/filter-choices
                                                                    anchor-point-choices
                                                                    [:top-left :bottom-right :angle]))
                                       [:anchor :point :default] (case (-> field :origin :point (or :top-left))
                                                                   :top-left :fess
                                                                   :bottom-right :fess
                                                                   :top-left)}))
            :per-bend-sinister (options/pick default-options
                                             [[:type]
                                              [:inherit-environment?]
                                              [:counterchanged?]
                                              [:line]
                                              [:origin]
                                              [:anchor]
                                              [:outline?]]
                                             (let [useful-points #{:top-right :bottom-left
                                                                   :chief :honour :fess :nombril :base}
                                                   point-choices (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  useful-points)
                                                   anchor-point-choices (util/filter-choices
                                                                         position/anchor-point-choices
                                                                         (conj useful-points :angle))]
                                               {[:line] line-style
                                                [:origin :point :choices] point-choices
                                                [:origin :point :default] :top-left
                                                [:anchor :point :choices] (case (-> field :origin :point (or :top-right))
                                                                            :top-right (util/filter-choices
                                                                                        anchor-point-choices
                                                                                        #{:bottom-left
                                                                                          :chief :honour :fess :nombril :base :angle})
                                                                            :bottom-left (util/filter-choices
                                                                                          anchor-point-choices
                                                                                          #{:top-right
                                                                                            :chief :honour :fess :nombril :base :angle})
                                                                            (util/filter-choices
                                                                             anchor-point-choices
                                                                             [:top-right :bottom-left :angle]))
                                                [:anchor :point :default] (case (-> field :origin :point (or :top-right))
                                                                            :top-right :fess
                                                                            :bottom-left :fess
                                                                            :top-right)}))
            :per-chevron (options/pick default-options
                                       [[:type]
                                        [:inherit-environment?]
                                        [:counterchanged?]
                                        [:line]
                                        [:opposite-line]
                                        [:origin]
                                        [:direction-anchor]
                                        [:anchor]
                                        [:outline?]]
                                       {[:line] (-> line-style
                                                    (options/override-if-exists [:offset :min] 0)
                                                    (options/override-if-exists [:base-line] nil))
                                        [:opposite-line] (-> opposite-line-style
                                                             (options/override-if-exists [:offset :min] 0)
                                                             (options/override-if-exists [:base-line] nil))
                                        [:direction-anchor :point :choices] (util/filter-choices
                                                                             position/anchor-point-choices
                                                                             [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle])
                                        [:direction-anchor :point :default] :bottom
                                        [:anchor :point :choices] (util/filter-choices
                                                                   position/anchor-point-choices
                                                                   (case (-> field :direction-anchor :point (or :bottom))
                                                                     :bottom [:bottom-left :bottom :bottom-right :left :right :angle]
                                                                     :top [:top-left :top :top-right :left :right :angle]
                                                                     :left [:top-left :left :bottom-left :top :bottom :angle]
                                                                     :right [:top-right :right :bottom-right :top :bottom :angle]
                                                                     :bottom-left [:bottom-left :bottom :bottom-right :top-left :left :angle]
                                                                     :bottom-right [:bottom-left :bottom :bottom-right :right :top-right :angle]
                                                                     :top-left [:top-left :top :top-right :left :bottom-left :angle]
                                                                     :top-right [:top-left :top :top-right :left :bottom-right :angle]
                                                                     [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle]))
                                        [:anchor :point :default] (case (-> field :direction-anchor :point (or :bottom))
                                                                    :bottom :bottom-left
                                                                    :top :top-right
                                                                    :left :top-left
                                                                    :right :bottom-right
                                                                    :bottom-left :left
                                                                    :bottom-right :bottom
                                                                    :top-left :top
                                                                    :top-right :right
                                                                    :angle :angle
                                                                    :bottom-left)})
            :per-pile (options/pick default-options
                                    [[:type]
                                     [:inherit-environment?]
                                     [:counterchanged?]
                                     [:origin]
                                     [:anchor]
                                     [:line]
                                     [:opposite-line]
                                     [:outline?]]
                                    (let [anchor-points #{:top-left :top :top-right
                                                          :left :right
                                                          :bottom-left :bottom :bottom-right
                                                          :fess :honour :nombril :base :chief
                                                          :angle}]
                                      {[:line] (-> line-style
                                                   (options/override-if-exists [:offset :min] 0)
                                                   (options/override-if-exists [:base-line] nil))
                                       [:opposite-line] (-> opposite-line-style
                                                            (options/override-if-exists [:offset :min] 0)
                                                            (options/override-if-exists [:base-line] nil))
                                       [:geometry] {:size {:type :range
                                                           :min 5
                                                           :max 100
                                                           :default (case (-> field :geometry :size-mode (or :thickness))
                                                                      :thickness 75
                                                                      30)
                                                           :ui (-> geometry/default-options :size :ui)}
                                                    :size-mode {:type :choice
                                                                :choices [["Thickness" :thickness]
                                                                          ["Angle" :angle]]
                                                                :default :thickness
                                                                :ui {:form-type :radio-select}}
                                                    :ui (-> geometry/default-options :ui)}
                                       [:origin :point :choices] (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  [:top-left :top :top-right
                                                                   :left :right
                                                                   :bottom-left :bottom :bottom-right])
                                       [:origin :point :default] :top
                                       [:origin :alignment] (:alignment position/default-options)
                                       [:anchor :point :choices] (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  (disj anchor-points (-> field :origin :point (or :top))))
                                       [:anchor :point :default] :fess
                                       [:anchor :alignment] nil
                                       [:anchor :angle :default] (cond
                                                                   (#{:top-left
                                                                      :top-right
                                                                      :bottom-left
                                                                      :bottom-right} (-> field :origin :point (or :top))) 45
                                                                   :else 0)
                                       [:anchor :angle :min] (cond
                                                               (#{:top-left
                                                                  :top-right
                                                                  :bottom-left
                                                                  :bottom-right} (-> field :origin :point (or :top))) 0
                                                               :else -90)
                                       [:anchor :angle :max] 90
                                       [:anchor :type] nil}))
            :per-saltire (options/pick default-options
                                       [[:type]
                                        [:inherit-environment?]
                                        [:counterchanged?]
                                        [:line]
                                        [:opposite-line]
                                        [:origin]
                                        [:anchor]
                                        [:outline?]]
                                       {[:line] (-> line-style
                                                    (options/override-if-exists [:offset :min] 0)
                                                    (options/override-if-exists [:base-line] nil)
                                                    (dissoc :fimbriation))
                                        [:opposite-line] (-> opposite-line-style
                                                             (options/override-if-exists [:offset :min] 0)
                                                             (options/override-if-exists [:base-line] nil)
                                                             (dissoc :fimbriation))
                                        [:origin :alignment] nil
                                        [:anchor :point :choices] (util/filter-choices
                                                                   position/anchor-point-choices
                                                                   [:top-left :top-right :bottom-left :bottom-right :angle])})
            :quartered (options/pick default-options
                                     [[:type]
                                      [:inherit-environment?]
                                      [:counterchanged?]
                                      [:line]
                                      [:opposite-line]
                                      [:origin :point]
                                      [:origin :offset-x]
                                      [:origin :offset-y]
                                      [:outline?]]
                                     {[:line] (-> line-style
                                                  (options/override-if-exists [:offset :min] 0)
                                                  (options/override-if-exists [:base-line] nil)
                                                  (dissoc :fimbriation))
                                      [:opposite-line] (-> opposite-line-style
                                                           (options/override-if-exists [:offset :min] 0)
                                                           (options/override-if-exists [:base-line] nil)
                                                           (dissoc :fimbriation))})
            :quarterly (options/pick default-options
                                     [[:type]
                                      [:inherit-environment?]
                                      [:counterchanged?]
                                      [:layout :num-base-fields]
                                      [:layout :num-fields-x]
                                      [:layout :offset-x]
                                      [:layout :stretch-x]
                                      [:layout :num-fields-y]
                                      [:layout :offset-y]
                                      [:layout :stretch-y]
                                      [:outline?]]
                                     {[:layout :num-fields-x :default] 3
                                      [:layout :num-fields-y :default] 4})
            :gyronny (options/pick default-options
                                   [[:type]
                                    [:inherit-environment?]
                                    [:counterchanged?]
                                    [:line]
                                    [:opposite-line]
                                    [:origin]
                                    [:anchor]
                                    [:outline?]]
                                   {[:line] (-> line-style
                                                (options/override-if-exists [:offset :min] 0)
                                                (options/override-if-exists [:base-line] nil)
                                                (dissoc :fimbriation))
                                    [:opposite-line] (-> opposite-line-style
                                                         (options/override-if-exists [:offset :min] 0)
                                                         (options/override-if-exists [:base-line] nil)
                                                         (dissoc :fimbriation))
                                    [:origin :alignment] nil
                                    [:anchor :point :choices] (util/filter-choices
                                                               position/anchor-point-choices
                                                               [:top-left :top-right :bottom-left :bottom-right :angle])})
            :paly (options/pick default-options
                                [[:type]
                                 [:inherit-environment?]
                                 [:counterchanged?]
                                 [:line]
                                 [:layout :num-base-fields]
                                 [:layout :num-fields-x]
                                 [:layout :offset-x]
                                 [:layout :stretch-x]
                                 [:outline?]]
                                {[:line] line-style
                                 [:line :fimbriation] nil})
            :barry (options/pick default-options
                                 [[:type]
                                  [:inherit-environment?]
                                  [:counterchanged?]
                                  [:line]
                                  [:layout :num-base-fields]
                                  [:layout :num-fields-y]
                                  [:layout :offset-y]
                                  [:layout :stretch-y]
                                  [:outline?]]
                                 {[:line] line-style
                                  [:line :fimbriation] nil})
            :chequy (options/pick default-options
                                  [[:type]
                                   [:inherit-environment?]
                                   [:counterchanged?]
                                   [:layout :num-base-fields]
                                   [:layout :num-fields-x]
                                   [:layout :offset-x]
                                   [:layout :stretch-x]
                                   [:layout :num-fields-y]
                                   [:layout :offset-y]
                                   [:layout :stretch-y]
                                   [:outline?]]
                                  {})
            :lozengy (options/pick default-options
                                   [[:type]
                                    [:inherit-environment?]
                                    [:counterchanged?]
                                    [:layout :num-fields-x]
                                    [:layout :offset-x]
                                    [:layout :stretch-x]
                                    [:layout :num-fields-y]
                                    [:layout :offset-y]
                                    [:layout :stretch-y]
                                    [:layout :rotation]
                                    [:outline?]]
                                   {[:layout :num-fields-y :default] nil
                                    [:layout :stretch-y :max] 3})
            :vairy (options/pick default-options
                                 [[:type]
                                  [:inherit-environment?]
                                  [:counterchanged?]
                                  [:variant]
                                  [:layout :num-fields-x]
                                  [:layout :offset-x]
                                  [:layout :stretch-x]
                                  [:layout :num-fields-y]
                                  [:layout :offset-y]
                                  [:layout :stretch-y]]
                                 {[:layout :num-fields-y :default] nil})
            :potenty (options/pick default-options
                                   [[:type]
                                    [:inherit-environment?]
                                    [:counterchanged?]
                                    [:variant]
                                    [:layout :num-fields-x]
                                    [:layout :offset-x]
                                    [:layout :stretch-x]
                                    [:layout :num-fields-y]
                                    [:layout :offset-y]
                                    [:layout :stretch-y]
                                    [:outline?]]
                                   {[:layout :num-fields-y :default] nil
                                    [:variant :choices] [["Default" :default]
                                                         ["Counter" :counter]
                                                         ["In pale" :in-pale]
                                                         ["En point" :en-point]]})
            :papellony (options/pick default-options
                                     [[:type]
                                      [:inherit-environment?]
                                      [:counterchanged?]
                                      [:thickness]
                                      [:layout :num-fields-x]
                                      [:layout :offset-x]
                                      [:layout :stretch-x]
                                      [:layout :num-fields-y]
                                      [:layout :offset-y]
                                      [:layout :stretch-y]
                                      [:outline?]]
                                     {[:layout :num-fields-y :default] nil})
            :masonry (options/pick default-options
                                   [[:type]
                                    [:inherit-environment?]
                                    [:counterchanged?]
                                    [:thickness]
                                    [:layout :num-fields-x]
                                    [:layout :offset-x]
                                    [:layout :stretch-x]
                                    [:layout :num-fields-y]
                                    [:layout :offset-y]
                                    [:layout :stretch-y]
                                    [:outline?]]
                                   {[:layout :num-fields-y :default] nil})
            :bendy (options/pick default-options
                                 [[:type]
                                  [:inherit-environment?]
                                  [:counterchanged?]
                                  [:line]
                                  [:layout :num-base-fields]
                                  [:layout :num-fields-y]
                                  [:layout :offset-y]
                                  [:layout :stretch-y]
                                  [:origin]
                                  [:anchor]
                                  [:outline?]]
                                 (let [useful-points #{:top-left :bottom-right
                                                       :chief :honour :fess :nombril :base}
                                       point-choices (util/filter-choices
                                                      position/anchor-point-choices
                                                      useful-points)
                                       anchor-point-choices (util/filter-choices
                                                             position/anchor-point-choices
                                                             (conj useful-points :angle))]
                                   {[:line] (-> line-style
                                                (dissoc :fimbriation))
                                    [:origin :point :choices] point-choices
                                    [:origin :point :default] :top-left
                                    [:anchor :point :choices] (case (-> field :origin :point (or :top-left))
                                                                :top-left (util/filter-choices
                                                                           anchor-point-choices
                                                                           #{:bottom-right
                                                                             :chief :honour :fess :nombril :base :angle})
                                                                :bottom-right (util/filter-choices
                                                                               anchor-point-choices
                                                                               #{:top-left
                                                                                 :chief :honour :fess :nombril :base :angle})
                                                                (util/filter-choices
                                                                 anchor-point-choices
                                                                 [:top-left :bottom-right :angle]))
                                    [:anchor :point :default] (case (-> field :origin :point (or :top-left))
                                                                :top-left :fess
                                                                :bottom-right :fess
                                                                :top-left)}))
            :bendy-sinister (options/pick default-options
                                          [[:type]
                                           [:inherit-environment?]
                                           [:counterchanged?]
                                           [:line]
                                           [:layout :num-base-fields]
                                           [:layout :num-fields-y]
                                           [:layout :offset-y]
                                           [:layout :stretch-y]
                                           [:origin]
                                           [:anchor]
                                           [:outline?]]
                                          (let [useful-points #{:top-right :bottom-left
                                                                :chief :honour :fess :nombril :base}
                                                point-choices (util/filter-choices
                                                               position/anchor-point-choices
                                                               useful-points)
                                                anchor-point-choices (util/filter-choices
                                                                      position/anchor-point-choices
                                                                      (conj useful-points :angle))]
                                            {[:line] (-> line-style
                                                         (dissoc :fimbriation))
                                             [:origin :point :choices] point-choices
                                             [:origin :point :default] :top-left
                                             [:anchor :point :choices] (case (-> field :origin :point (or :top-right))
                                                                         :top-right (util/filter-choices
                                                                                     anchor-point-choices
                                                                                     #{:bottom-left
                                                                                       :chief :honour :fess :nombril :base :angle})
                                                                         :bottom-left (util/filter-choices
                                                                                       anchor-point-choices
                                                                                       #{:top-right
                                                                                         :chief :honour :fess :nombril :base :angle})
                                                                         (util/filter-choices
                                                                          anchor-point-choices
                                                                          [:top-right :bottom-left :angle]))
                                             [:anchor :point :default] (case (-> field :origin :point (or :top-right))
                                                                         :top-right :fess
                                                                         :bottom-left :fess
                                                                         :top-right)}))
            :tierced-per-pale (options/pick default-options
                                            [[:type]
                                             [:inherit-environment?]
                                             [:counterchanged?]
                                             [:line]
                                             [:layout :stretch-x]
                                             [:origin :point]
                                             [:origin :offset-x]
                                             [:outline?]]
                                            {[:origin :point :choices] position/point-choices-x
                                             [:line] line-style
                                             [:line :fimbriation] nil})
            :tierced-per-fess (options/pick default-options
                                            [[:type]
                                             [:inherit-environment?]
                                             [:counterchanged?]
                                             [:line]
                                             [:layout :stretch-y]
                                             [:origin :point]
                                             [:origin :offset-y]
                                             [:outline?]]
                                            {[:origin :point :choices] position/point-choices-y
                                             [:line] line-style
                                             [:line :fimbriation] nil})
            :tierced-per-pairle (options/pick default-options
                                              [[:type]
                                               [:inherit-environment?]
                                               [:counterchanged?]
                                               [:line]
                                               [:opposite-line]
                                               [:extra-line]
                                               [:origin]
                                               [:anchor]
                                               [:outline?]]
                                              {[:line] (-> line-style
                                                           (options/override-if-exists [:offset :min] 0)
                                                           (options/override-if-exists [:base-line] nil)
                                                           (dissoc :fimbriation))
                                               [:opposite-line] (-> opposite-line-style
                                                                    (options/override-if-exists [:offset :min] 0)
                                                                    (options/override-if-exists [:base-line] nil)
                                                                    (dissoc :fimbriation))
                                               [:extra-line] (-> extra-line-style
                                                                 (options/override-if-exists [:offset :min] 0)
                                                                 (options/override-if-exists [:base-line] nil)
                                                                 (dissoc :fimbriation))
                                               [:anchor :point :choices] (util/filter-choices
                                                                          position/anchor-point-choices
                                                                          [:top-left :top-right :angle])})
            :tierced-per-pairle-reversed (options/pick default-options
                                                       [[:type]
                                                        [:inherit-environment?]
                                                        [:counterchanged?]
                                                        [:line]
                                                        [:opposite-line]
                                                        [:extra-line]
                                                        [:origin]
                                                        [:anchor]
                                                        [:outline?]]
                                                       {[:line] (-> line-style
                                                                    (options/override-if-exists [:offset :min] 0)
                                                                    (options/override-if-exists [:base-line] nil)
                                                                    (dissoc :fimbriation))
                                                        [:opposite-line] (-> opposite-line-style
                                                                             (options/override-if-exists [:offset :min] 0)
                                                                             (options/override-if-exists [:base-line] nil)
                                                                             (dissoc :fimbriation))
                                                        [:extra-line] (-> extra-line-style
                                                                          (options/override-if-exists [:offset :min] 0)
                                                                          (options/override-if-exists [:base-line] nil)
                                                                          (dissoc :fimbriation))
                                                        [:anchor :point :choices] (util/filter-choices
                                                                                   position/anchor-point-choices
                                                                                   [:bottom-left :bottom-right :angle])})

            {})
          (update :anchor (fn [anchor]
                            (when anchor
                              (position/adjust-options anchor (-> field :anchor)))))
          (update :direction-anchor (fn [direction-anchor]
                                      (when direction-anchor
                                        (position/adjust-options direction-anchor (-> field :direction-anchor)))))
          ;; TODO: all this position post processing can surely be done smarter
          (update :origin (fn [position]
                            (when position
                              (-> position
                                  (position/adjust-options (-> field :origin))
                                  (assoc :ui (-> default-options :origin :ui))))))
          (update :direction-anchor (fn [position]
                                      (when position
                                        (-> position
                                            (position/adjust-options (-> field :direction-anchor))
                                            (assoc :ui (-> default-options :direction-anchor :ui))))))
          (update :anchor (fn [position]
                            (when position
                              (-> position
                                  (position/adjust-options (-> field :anchor))
                                  (assoc :ui (-> default-options :anchor :ui))))))
          (update :layout (fn [layout]
                            (when layout
                              (assoc layout :ui (-> default-options :layout :ui)))))))))

(defmethod interface/component-options :field [data path]
  (cond-> (options data)
    (-> path drop-last last
        (= :coat-of-arms)) (->
                            (dissoc :inherit-environment?)
                            (dissoc :counterchanged?))))
